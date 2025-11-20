# Luồng Hoạt Động của AI Service

## Tổng Quan

AI Service là một microservice Spring Boot (WebFlux - Reactive) cung cấp 2 chức năng chính:
1. **Chat Advisor**: Tư vấn tài chính thông minh với lịch sử conversation
2. **Daily Reports**: Tạo báo cáo tóm tắt tài chính & học tập hàng ngày

**Công nghệ:**
- Spring Boot 3.5.7 (WebFlux - Reactive)
- Java 21
- MySQL Database
- Google Gemini AI API (gemini-2.5-flash)

---

## 1. Luồng Chat Advisor (POST /api/chat/ask)

### 1.1. Sơ Đồ Luồng

```
Client Request
    ↓
ChatController.ask()
    ↓
[1] Xử lý conversationId
    ├─ Có conversationId → Lấy lịch sử (10 messages gần nhất)
    └─ Không có → Tạo conversationId mới
    ↓
[2] Fetch dữ liệu từ downstream services (song song)
    ├─ Transaction Service (timeout: 5s)
    ├─ User Profile Service (timeout: 5s)
    ├─ Goals Service (timeout: 5s)
    └─ Learning Service (timeout: 5s)
    ↓
[3] Xây dựng Prompt
    ├─ PromptBuilder.buildChatPrompt()
    ├─ Bao gồm: question, userData, conversationHistory, systemContext
    └─ Format: JSON với answer, tips, disclaimers
    ↓
[4] Gọi Gemini API
    ├─ GeminiClient.callGemini(prompt)
    ├─ Timeout: 60s
    └─ Trả về: Result (answerText, usage, model)
    ↓
[5] Xử lý Response
    ├─ OutputGuard.filterViolations() → Sanitize (mask CC, SSN, profanity)
    ├─ ChatResponseFormatter.formatResponse() → Parse JSON, extract answer/tips/disclaimers
    └─ Tạo ChatResponse object
    ↓
[6] Lưu vào Database
    ├─ ChatHistoryService.saveMessage()
    ├─ Lưu: conversationId, userId, question, prompt, answer, tokens, timestamps
    └─ Đợi save xong trước khi trả về (để conversation xuất hiện ngay)
    ↓
[7] Trả về Response cho Client
    └─ ChatResponse (JSON)
```

### 1.2. Chi Tiết Các Bước

#### Bước 1: Xử lý Conversation ID

**File:** `ChatController.java` (dòng 74-84)

```java
// Lấy hoặc tạo conversation ID
String conversationId = Optional.ofNullable(req.getConversationId())
        .filter(id -> !id.trim().isEmpty())
        .orElseGet(() -> chatHistoryService.generateConversationId());

// Lấy conversation history nếu có conversationId
Mono<String> historyMono = conversationId != null && !conversationId.isEmpty()
        ? chatHistoryService.getConversationContext(conversationId, 10)
                .map(messages -> formatHistoryForPrompt(messages))
                .defaultIfEmpty("")
        : Mono.just("");
```

**Logic:**
- Nếu có `conversationId` → Lấy 10 messages gần nhất từ database
- Nếu không có → Tạo UUID mới
- Format history thành text để đưa vào prompt

#### Bước 2: Fetch Dữ liệu từ Downstream Services

**File:** `ChatController.java` (dòng 86-91, 235-249)

```java
Mono.zip(
    fetchOptional(transactionServiceUrl).defaultIfEmpty(Map.of()),
    fetchOptional(userProfileServiceUrl).defaultIfEmpty(Map.of()),
    fetchOptional(goalsServiceUrl).defaultIfEmpty(Map.of()),
    fetchOptional(learningServiceUrl).defaultIfEmpty(Map.of()),
    historyMono
)
```

**Logic:**
- Fetch song song 4 services (transaction, userprofile, goals, learning)
- Timeout: 5 giây mỗi service
- Nếu service không available → Trả về `Map.of()` (empty)
- Không block luồng chính nếu service lỗi

#### Bước 3: Xây dựng Prompt

**File:** `PromptBuilder.java` (dòng 40-78)

**Input:**
- `question`: Câu hỏi của user
- `userData`: Dữ liệu từ downstream services
- `conversationHistory`: Lịch sử conversation (10 messages gần nhất)
- `systemContext`: Timestamp, metadata

**Output:** Prompt string với format:
```
Bạn là cố vấn tài chính thông minh của EduFinAI...
Lịch sử cuộc hội thoại trước đó:
User: ...
Assistant: ...
---
Câu hỏi hiện tại: ...
Dữ liệu người dùng: ...
Yêu cầu trả về JSON: {"answer": "...", "tips": [...], "disclaimers": [...]}
```

#### Bước 4: Gọi Gemini API

**File:** `GeminiClient.java` (dòng 57-111)

**Request:**
- URL: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent`
- Method: POST
- Body: `{"contents": [{"parts": [{"text": prompt}]}]}`
- Query: `?key={GEMINI_API_KEY}`

**Response Parsing:**
- Extract `text` từ `candidates[0].content.parts[0].text`
- Extract `usageMetadata` (promptTokens, candidatesTokens, totalTokens)
- Handle errors (4xx, 5xx, timeout, empty response)

**Timeout:** 60 giây (có thể cấu hình)

#### Bước 5: Xử lý Response

**5.1. Sanitize (OutputGuard)**

**File:** `OutputGuard.java` (dòng 21-44)

**Các pattern bị mask:**
- Credit Card (13-19 chữ số) → `####-MASKED`
- SSN (9 chữ số) → `###-MASKED`
- Profanity (đm, dm, cc, shit, fuck) → `***`

**5.2. Format Response (ChatResponseFormatter)**

**File:** `ChatResponseFormatter.java` (dòng 33-76)

**Logic:**
- Extract JSON từ markdown code block (nếu có): ````json ... ````
- Parse JSON: `{"answer": "...", "tips": [...], "disclaimers": [...]}`
- Tạo `ChatResponse` object với các field:
  - `answer`: Câu trả lời chính
  - `tips`: Danh sách mẹo (1-3 mẹo)
  - `disclaimers`: Danh sách lưu ý (1-2 lưu ý)

#### Bước 6: Lưu vào Database

**File:** `ChatHistoryService.java` (dòng 55-76)

**Entity:** `AiLogEntity`
- `conversationId`: UUID của conversation
- `userId`: ID của user
- `question`: Câu hỏi
- `prompt`: Prompt đã gửi đến Gemini
- `rawAnswer`: Response gốc từ Gemini
- `sanitizedAnswer`: Response đã sanitize (JSON)
- `formattedAnswer`: Answer đã format (text)
- `usagePromptTokens`, `usageCompletionTokens`, `usageTotalTokens`
- `createdAt`: UTC timestamp

**Lưu ý:** Sử dụng `thenReturn()` để đợi save xong trước khi trả về response, đảm bảo conversation xuất hiện ngay trong danh sách.

#### Bước 7: Trả về Response

**File:** `ChatController.java` (dòng 130-156)

**Response Format:**
```json
{
  "userId": "user123",
  "question": "Tôi nên tiết kiệm bao nhiêu?",
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "answer": "Chào bạn! Tôi là cố vấn tài chính...",
  "tips": ["Mẹo 1", "Mẹo 2"],
  "disclaimers": ["Lưu ý 1"],
  "model": "gemini-2.5-flash",
  "promptTokens": 150,
  "completionTokens": 200,
  "totalTokens": 350,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

## 2. Luồng Daily Report (Scheduler)

### 2.1. Sơ Đồ Luồng

```
Scheduler Trigger (Cron: 0 0 0 * * * - 00:00 UTC hàng ngày)
    ↓
DataFetchScheduler.runDaily()
    ↓
[1] Fetch dữ liệu từ downstream services (song song)
    ├─ Transaction Service (timeout: 8s)
    ├─ User Profile Service (timeout: 8s)
    ├─ Goals Service (timeout: 8s)
    └─ Learning Service (timeout: 8s)
    ↓
[2] Xây dựng Prompt
    ├─ PromptBuilder.buildDailyReportPrompt()
    ├─ Input: reportDate, transactions, userProfile, goals, learning
    └─ Format: JSON với highlights, risks, kpis, advice
    ↓
[3] Gọi Gemini API
    ├─ GeminiClient.callGemini(prompt)
    ├─ Timeout: 60s
    └─ Trả về: Result (answerText, usage, model)
    ↓
[4] Xử lý Response
    ├─ OutputGuard.filterViolations() → Sanitize
    └─ Tạo metadata (violations, flags)
    ↓
[5] Lưu vào Database
    ├─ ReportService.saveSanitizedReport()
    ├─ Lưu: reportDate, prompt, rawSummary, sanitizedSummary, tokens, metadata
    └─ Entity: ReportEntity
    ↓
[6] Log kết quả
    └─ Log: reportDate, model, tokens
```

### 2.2. Chi Tiết Các Bước

#### Bước 1: Fetch Dữ liệu

**File:** `DataFetchScheduler.java` (dòng 63-71)

**Logic:**
- Fetch song song 4 services
- Timeout: 8 giây mỗi service (dài hơn chat vì không cần real-time)
- Nếu service không available → Dùng `Map.of()` (empty)

#### Bước 2: Xây dựng Prompt

**File:** `PromptBuilder.java` (dòng 25-38)

**Input:**
- `reportDate`: Ngày của report (UTC)
- `transactions`: Dữ liệu giao dịch
- `userProfile`: Hồ sơ người dùng
- `goals`: Mục tiêu tài chính
- `learning`: Hoạt động học tập

**Output:** Prompt string với format:
```
Bạn là Data Analyst của ứng dụng EduFinAI.
Hãy tạo báo cáo tóm tắt tài chính & học tập cho ngày {date}.
- Tổng quan giao dịch: {...}
- Hồ sơ người dùng: {...}
- Mục tiêu tài chính: {...}
- Hoạt động học tập: {...}
Đầu ra yêu cầu JSON: {"highlights": [...], "risks": [...], "kpis": {...}, "advice": [...]}
```

#### Bước 3-4: Gọi Gemini & Xử lý Response

Tương tự như Chat Flow (Bước 4-5)

#### Bước 5: Lưu vào Database

**File:** `ReportService.java` (dòng 43-72)

**Entity:** `ReportEntity`
- `reportDate`: DATE (UNIQUE) - Ngày của report
- `prompt`: Prompt đã gửi
- `rawSummary`: Summary gốc từ Gemini
- `sanitizedSummary`: Summary đã sanitize (JSON)
- `model`: Model Gemini
- `usagePromptTokens`, `usageCompletionTokens`, `usageTotalTokens`
- `metadataJson`: Metadata (violations, flags)
- `createdAt`, `updatedAt`: UTC timestamps

**Lưu ý:** 
- `reportDate` được normalize về UTC với time = 00:00:00
- Nếu report đã tồn tại → Update, nếu chưa → Create

---

## 3. Luồng Quản Lý Conversation

### 3.1. Get User Conversations

**Endpoint:** `GET /api/chat/conversations?userId={userId}`

**File:** `ChatController.java` (dòng 183-199), `ChatHistoryService.java` (dòng 106-185)

**Luồng:**
```
Request
    ↓
ChatController.getConversations()
    ↓
ChatHistoryService.getUserConversations(userId)
    ↓
[1] Query database
    ├─ AiLogRepository.findConversationIdsWithLastUpdatedByUserId()
    └─ GROUP BY conversationId, MAX(createdAt) as lastUpdated
    ↓
[2] Build ConversationSummary cho mỗi conversation
    ├─ conversationId
    ├─ userId
    ├─ title: Câu hỏi đầu tiên (max 100 chars)
    ├─ messageCount: Số messages
    ├─ createdAt: Thời gian tạo (UTC)
    ├─ updatedAt: Thời gian message cuối (UTC)
    └─ relativeTime: "Vừa xong", "2 phút trước", "Hôm qua", ...
    ↓
[3] Trả về danh sách
    └─ List<ConversationSummary>
```

**Relative Time Format:**
- < 1 phút: "Vừa xong"
- < 1 giờ: "X phút trước"
- < 24 giờ: "X giờ trước"
- Hôm qua: "Hôm qua"
- < 7 ngày: "X ngày trước"
- < 4 tuần: "X tuần trước"
- < 12 tháng: "X tháng trước"
- > 1 năm: "dd/MM/yyyy"

### 3.2. Get Conversation History

**Endpoint:** `GET /api/chat/conversations/{conversationId}`

**File:** `ChatController.java` (dòng 204-211), `ChatHistoryService.java` (dòng 81-101)

**Luồng:**
```
Request
    ↓
ChatController.getConversationHistory(conversationId)
    ↓
ChatHistoryService.getConversationHistory(conversationId)
    ↓
[1] Query database
    ├─ AiLogRepository.findByConversationIdOrderByCreatedAtAsc()
    └─ Lấy tất cả messages, sắp xếp theo thời gian (cũ → mới)
    ↓
[2] Convert Entity → DTO
    ├─ toChatMessage() cho mỗi entity
    ├─ Parse tips/disclaimers từ sanitizedAnswer (JSON)
    └─ Validate timestamps (UTC)
    ↓
[3] Build ConversationHistory
    ├─ conversationId
    ├─ userId
    └─ messages: List<ChatMessage>
    ↓
[4] Trả về response
    └─ ConversationHistory
```

### 3.3. Delete Conversation

**Endpoint:** `DELETE /api/chat/conversations/{conversationId}`

**File:** `ChatController.java` (dòng 216-233), `ChatHistoryService.java` (dòng 212-233)

**Luồng:**
```
Request
    ↓
ChatController.deleteConversation(conversationId)
    ↓
ChatHistoryService.deleteConversation(conversationId)
    ↓
[1] Kiểm tra conversation có tồn tại
    ├─ AiLogRepository.countByConversationId()
    └─ Nếu count = 0 → Return false (404)
    ↓
[2] Xóa tất cả messages
    ├─ TransactionTemplate.execute() (đảm bảo transaction safety)
    ├─ AiLogRepository.deleteByConversationId()
    └─ Log số messages đã xóa
    ↓
[3] Trả về response
    └─ {"success": true, "message": "...", "conversationId": "..."}
```

**Lưu ý:** Sử dụng `TransactionTemplate` để đảm bảo transaction safety trong reactive context.

---

## 4. Luồng Get Daily Report

**Endpoint:** `GET /api/reports/daily?date={date}`

**File:** `ReportController.java` (dòng 33-45), `ReportService.java` (dòng 30-41)

**Luồng:**
```
Request
    ↓
ReportController.getDailyReport(date)
    ↓
ReportService.getDailyReport(date)
    ↓
[1] Normalize date
    ├─ Nếu date = null → Dùng hôm nay
    └─ Convert về LocalDate (UTC)
    ↓
[2] Query database
    ├─ ReportRepository.findByReportDate(date)
    └─ Nếu không tìm thấy → Return null (404)
    ↓
[3] Convert Entity → DTO
    ├─ toResponse()
    └─ ReportResponse với tất cả fields
    ↓
[4] Trả về response
    └─ ReportResponse
```

---

## 5. Luồng Generate Daily Report (Manual)

**Endpoint:** `POST /api/reports/daily/generate`

**File:** `ReportController.java` (dòng 51-63)

**Luồng:**
```
Request
    ↓
ReportController.generateDailyReport()
    ↓
[1] Trigger scheduler
    ├─ DataFetchScheduler.runDaily()
    └─ Chạy trong background thread (boundedElastic)
    ↓
[2] Đợi 3 giây
    └─ delayElement(Duration.ofSeconds(3))
    ↓
[3] Lấy report vừa tạo
    ├─ ReportService.getDailyReport(ZonedDateTime.now())
    └─ Nếu không tìm thấy → Return 500 error
    ↓
[4] Trả về response
    └─ ReportResponse
```

**Lưu ý:** 
- Endpoint này chạy bất đồng bộ
- Có delay 3 giây để đợi scheduler hoàn thành
- Nếu không tạo được report → Trả về 500 error

---

## 6. Kiến Trúc Component

### 6.1. Controller Layer

- **ChatController**: Xử lý chat requests (ask, get conversations, delete)
- **ReportController**: Xử lý report requests (get, generate)

### 6.2. Service Layer

- **ChatHistoryService**: Quản lý conversation history (save, get, delete)
- **ChatResponseFormatter**: Format response từ Gemini (parse JSON, extract fields)
- **OutputGuard**: Sanitize output (mask CC, SSN, profanity)
- **ReportService**: Quản lý daily reports (save, get)

### 6.3. Integration Layer

- **GeminiClient**: Gọi Gemini API (WebClient reactive)

### 6.4. Processor Layer

- **PromptBuilder**: Xây dựng prompt cho chat và daily report

### 6.5. Scheduler Layer

- **DataFetchScheduler**: Scheduler tự động tạo daily report (cron: 0 0 0 * * *)

### 6.6. Repository Layer

- **AiLogRepository**: CRUD cho `ai_logs` table
- **ReportRepository**: CRUD cho `ai_reports` table

### 6.7. Model Layer

- **AiLogEntity**: Entity cho chat messages
- **ReportEntity**: Entity cho daily reports

---

## 7. Database Schema

### 7.1. Table: `ai_logs`

Lưu trữ lịch sử chat conversations:

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `conversation_id` | VARCHAR(64) | ID của conversation (UUID) |
| `user_id` | VARCHAR(64) | ID của user |
| `question` | LONGTEXT | Câu hỏi của user |
| `prompt` | LONGTEXT | Prompt đã gửi đến Gemini |
| `model` | VARCHAR(100) | Model Gemini |
| `raw_answer` | LONGTEXT | Response gốc từ Gemini |
| `sanitized_answer` | LONGTEXT | Response đã sanitize (JSON) |
| `formatted_answer` | LONGTEXT | Answer đã format (text) |
| `usage_prompt_tokens` | INT | Số token trong prompt |
| `usage_completion_tokens` | INT | Số token trong response |
| `usage_total_tokens` | INT | Tổng số token |
| `created_at` | TIMESTAMP | Thời gian tạo (UTC) |

**Indexes:**
- `idx_ai_logs_user_time`: (user_id, created_at)
- `idx_ai_logs_conversation`: (conversation_id, created_at)

### 7.2. Table: `ai_reports`

Lưu trữ daily reports:

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `report_date` | DATE | Ngày của report (UNIQUE) |
| `model` | VARCHAR(100) | Model Gemini |
| `prompt` | LONGTEXT | Prompt đã gửi |
| `raw_summary` | LONGTEXT | Summary gốc từ Gemini |
| `sanitized_summary` | LONGTEXT | Summary đã sanitize (JSON) |
| `usage_prompt_tokens` | INT | Số token trong prompt |
| `usage_completion_tokens` | INT | Số token trong response |
| `usage_total_tokens` | INT | Tổng số token |
| `metadata_json` | LONGTEXT | Metadata (JSON) |
| `created_at` | TIMESTAMP | Thời gian tạo (UTC) |
| `updated_at` | TIMESTAMP | Thời gian cập nhật (UTC) |

---

## 8. Cấu Hình

### 8.1. Application Configuration

**File:** `application.yaml`

**Key Configurations:**
- **Database**: MySQL connection (localhost:3306/ai_service)
- **Gemini API**: URL, API key (env: `GEMINI_API_KEY`), model (`gemini-2.5-flash`)
- **Downstream Services**: URLs cho transaction, userprofile, goals, learning
- **CORS**: Allowed origins (localhost:3000, localhost:3001)
- **Scheduler**: Cron expression (`0 0 0 * * *` - 00:00 UTC hàng ngày)
- **Timeouts**: 
  - Downstream services: 5s (chat), 8s (scheduler)
  - Gemini API: 60s

### 8.2. Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GEMINI_API_KEY` | Yes | API key cho Google Gemini API |

---

## 9. Error Handling

### 9.1. Global Exception Handler

**File:** `GlobalExceptionHandler.java`

**Xử lý:**
- Validation errors (400)
- Resource not found (404)
- Internal server errors (500)
- Gemini API errors

**Format:**
```json
{
  "code": "ERROR_CODE",
  "message": "Error message",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 9.2. Common Error Scenarios

1. **Validation Error (400)**: Question cannot be blank
2. **Resource Not Found (404)**: Conversation not found
3. **Gemini API Error (500)**: Service unavailable, timeout, empty response
4. **Internal Server Error (500)**: Database error, unexpected exception

---

## 10. Reactive Programming

Service sử dụng **Spring WebFlux (Reactive)** với các đặc điểm:

- **Mono<T>**: Single value async
- **Flux<T>**: Multiple values async
- **Non-blocking I/O**: Tất cả operations đều non-blocking
- **Backpressure**: Tự động xử lý backpressure
- **Schedulers**: 
  - `Schedulers.boundedElastic()`: Cho blocking operations (database)
  - `Schedulers.parallel()`: Cho CPU-intensive operations

**Lưu ý:**
- Database operations được wrap trong `Mono.fromCallable()` và chạy trên `boundedElastic` scheduler
- WebClient operations (Gemini API, downstream services) sử dụng reactive natively

---

## 11. Security & Sanitization

### 11.1. Output Guard

**File:** `OutputGuard.java`

**Các pattern bị mask:**
- Credit Card (13-19 chữ số) → `####-MASKED`
- SSN (9 chữ số) → `###-MASKED`
- Profanity (đm, dm, cc, shit, fuck) → `***`

**Flags:**
- `possible_cc_masked`: Có số thẻ tín dụng bị mask
- `possible_ssn_masked`: Có SSN bị mask
- `profanity_masked`: Có từ ngữ không phù hợp bị mask

### 11.2. CORS Configuration

**File:** `CorsConfig.java`

**Allowed Origins:**
- `http://localhost:3000`
- `http://localhost:3001`
- `http://127.0.0.1:3000`

**Allowed Methods:** GET, POST, PUT, DELETE, OPTIONS, PATCH

---

## 12. Logging

**File:** `application.yaml` (dòng 33-48)

**Configuration:**
- Log file: `logs/ai-service.log`
- Max size: 10MB
- Max history: 30 files
- Pattern: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n`

**Log Levels:**
- Root: INFO
- Spring WebFlux: WARN
- Reactor Netty: WARN
- Application: INFO

---

## 13. Timezone Handling

**Tất cả timestamps sử dụng UTC:**

- Database: `TIMESTAMP` columns lưu UTC
- Application: `ZonedDateTime` với `ZoneId.of("UTC")`
- Response: ISO 8601 format với UTC timezone

**Lưu ý:**
- `reportDate` được normalize về UTC với time = 00:00:00
- `createdAt`, `updatedAt` luôn là UTC
- Relative time được tính từ UTC

---

## 14. Performance Considerations

### 14.1. Timeouts

- **Downstream Services**: 5s (chat), 8s (scheduler)
- **Gemini API**: 60s
- **Database**: Default connection pool timeout

### 14.2. Async Operations

- **Chat Flow**: 
  - Fetch downstream services song song (Mono.zip)
  - Save message async nhưng đợi xong trước khi trả về
- **Scheduler**: 
  - Fetch downstream services song song
  - Blocking operation (block()) vì chạy trong background

### 14.3. Database Indexes

- `idx_ai_logs_user_time`: (user_id, created_at) - Cho query conversations của user
- `idx_ai_logs_conversation`: (conversation_id, created_at) - Cho query messages trong conversation

---

## 15. Testing

### 15.1. Test Chat API

```bash
# Tạo conversation mới
curl -X POST http://localhost:9001/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{"userId": "test-user", "question": "Tôi nên tiết kiệm bao nhiêu?"}'

# Tiếp tục conversation
curl -X POST http://localhost:9001/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{"userId": "test-user", "conversationId": "...", "question": "Có rủi ro gì không?"}'
```

### 15.2. Test Report API

```bash
# Lấy report hôm nay
curl -X GET "http://localhost:9001/api/reports/daily"

# Generate report thủ công
curl -X POST "http://localhost:9001/api/reports/daily/generate"
```

---

## 16. Tóm Tắt

### 16.1. Chat Flow

1. Nhận request → Xử lý conversationId
2. Fetch dữ liệu từ downstream services (song song)
3. Xây dựng prompt với context và history
4. Gọi Gemini API
5. Sanitize và format response
6. Lưu vào database
7. Trả về response

### 16.2. Daily Report Flow

1. Scheduler trigger (cron: 00:00 UTC)
2. Fetch dữ liệu từ downstream services (song song)
3. Xây dựng prompt cho daily report
4. Gọi Gemini API
5. Sanitize response
6. Lưu vào database
7. Log kết quả

### 16.3. Key Features

- ✅ Conversation History (giống ChatGPT)
- ✅ Context-aware responses (10 messages gần nhất)
- ✅ Data sanitization (mask CC, SSN, profanity)
- ✅ Structured response (answer, tips, disclaimers)
- ✅ Daily reports với scheduler
- ✅ Reactive programming (non-blocking)
- ✅ UTC timezone handling
- ✅ Error handling và logging

---

**Last Updated:** 2024-11-16

