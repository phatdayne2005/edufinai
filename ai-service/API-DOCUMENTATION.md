# API Documentation - EduFinAI Service

## Tổng quan

EduFinAI Service là một microservice cung cấp các API tích hợp với Google Gemini AI để:
- **Chat Advisor**: Tư vấn tài chính thông minh với lịch sử conversation (giống ChatGPT)
- **Daily Reports**: Tạo báo cáo tóm tắt tài chính & học tập hàng ngày

**Công nghệ:**
- Spring Boot 3.5.7 (WebFlux - Reactive)
- Java 21
- MySQL Database
- Google Gemini AI API

---

## Base URL

```
http://localhost:8080
```

**Lưu ý:** Port mặc định là 8080. Có thể thay đổi trong `application.yaml`.

---

## Authentication

- **Tất cả** endpoint (trừ `/actuator/health`) yêu cầu JWT hợp lệ do `auth-service` phát hành và được Gateway kiểm tra.
- Frontend phải gọi qua Gateway (`http://localhost:8080/...`) và đính kèm header `Authorization: Bearer <JWT>`.
- Token có thể lấy từ API `POST http://localhost:9000/identity/auth/token`.
- `ai-service` hoạt động như Resource Server (HS512), nên chỉ cần Gateway forward JWT là đủ.
- Đảm bảo cấu hình CORS/Gateway đã sẵn sàng để browser gửi kèm header Authorization.

---

## CORS Configuration

API hỗ trợ CORS cho các origin sau (có thể cấu hình trong `application.yaml`):
- `http://localhost:3000`
- `http://localhost:3001`
- `http://127.0.0.1:3000`

**Allowed Methods:** GET, POST, PUT, DELETE, OPTIONS, PATCH

**Allowed Headers:** Content-Type, Authorization, X-Requested-With, Accept, Origin

---

## Hướng dẫn nhanh cho Frontend

| Trang/tính năng FE | API cần gọi | Ghi chú quan trọng |
|--------------------|-------------|--------------------|
| Trang chủ → ô **Báo cáo hôm nay** | `GET /ai/reports/daily` | Lấy `insight`, `rootCause`, `priorityAction`. Nếu trống thì hiển thị thông báo “Chưa đủ dữ liệu…”. |
| Trang chủ → nút “Tạo báo cáo mới” | Gọi lại `GET /ai/reports/daily` | Không có endpoint POST, hệ thống tạo báo cáo on-demand mỗi lần gọi. |
| Trang cá nhân → thẻ **Tư vấn AI** (3 thẻ) | `POST /ai/chat/ask` với `context` = `SPENDING_WIDGET` / `SAVING_WIDGET` / `GOAL_WIDGET` | Không cần `question`, backend tự tạo prompt và *không lưu lịch sử*. |
| Trang **AI Chat** (chatbot toàn màn hình) | `POST /ai/chat/ask` với `question`, `conversationId` (khi tiếp tục) | `conversationId` do backend trả về ở lần hỏi đầu, FE lưu lại để tiếp tục chat. |
| Màn danh sách hội thoại | `GET /ai/chat/conversations` | Trả về danh sách conversation của user trong JWT. |
| Chi tiết hội thoại | `GET /ai/chat/conversations/{conversationId}` | Hiển thị toàn bộ lịch sử chat. |
| Xóa hội thoại | `DELETE /ai/chat/conversations/{conversationId}` | Sau khi xóa, conversation biến mất khỏi list FE. |

Tất cả request đều gửi qua Gateway `http://localhost:8080` và phải kèm header `Authorization: Bearer <JWT>`.

---

## API Endpoints

### 1. Chat API

#### 1.1. Ask Question

Gửi câu hỏi tư vấn tài chính và nhận phản hồi từ AI. API tự động lưu lịch sử conversation và có thể tiếp tục cuộc hội thoại cũ.

**Endpoint:** `POST /api/chat/ask`

**Request Headers:**
```
Authorization: Bearer <JWT>
Content-Type: application/json
Accept: application/json
```

**Request Body:**
```json
{
  "userId": "string (optional, sẽ bị override bởi JWT)",
  "conversationId": "string (optional)",
  "question": "string (required nếu không có context)",
  "context": "string (optional - preset cho widget)"
}
```

**Request Schema:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `userId` | string | No | Không còn sử dụng: backend luôn lấy userId từ JWT |
| `conversationId` | string | No | ID của conversation để tiếp tục cuộc hội thoại cũ. Nếu không có, sẽ tạo conversation mới |
| `question` | string | Yes* | Câu hỏi cần tư vấn. Có thể để trống nếu gửi `context` preset |
| `context` | string | No | Giá trị preset: `SPENDING_WIDGET`, `SAVING_WIDGET`, `GOAL_WIDGET` dùng để sinh nội dung cho 3 thẻ Tư vấn AI |

**Response:** `200 OK`

**Response Body:**
```json
{
  "userId": "string",
  "question": "string",
  "conversationId": "string",
  "answer": "string",
  "tips": ["string"],
  "disclaimers": ["string"],
  "model": "string",
  "promptTokens": 0,
  "completionTokens": 0,
  "totalTokens": 0,
  "createdAt": "2024-01-01T10:00:00+07:00"
}
```

**Response Schema:**
| Field | Type | Description |
|-------|------|-------------|
| `userId` | string | ID người dùng (được lấy từ JWT) |
| `question` | string | Câu hỏi đã gửi |
| `conversationId` | string | ID của conversation (dùng để tiếp tục cuộc hội thoại) |
| `answer` | string | Câu trả lời chính từ AI |
| `tips` | array[string] | Danh sách các mẹo/tips (1-3 mẹo) |
| `disclaimers` | array[string] | Danh sách các lưu ý/disclaimers (1-2 lưu ý) |
| `model` | string | Model Gemini được sử dụng (ví dụ: "gemini-2.5-flash") |
| `promptTokens` | integer | Số token trong prompt |
| `completionTokens` | integer | Số token trong response |
| `totalTokens` | integer | Tổng số token sử dụng |
| `createdAt` | string (ISO 8601) | Thời điểm tạo response |

**Cơ chế hoạt động:**
1. Nếu có `conversationId`, API sẽ lấy lịch sử conversation (10 messages gần nhất) và đưa vào context cho AI
2. Nếu không có `conversationId`, API sẽ tạo conversation mới
3. API tự động fetch dữ liệu từ các downstream services (nếu có):
   - Transaction Service: `http://finance-service/api/summary/daily`
   - User Profile Service: `http://auth-service/api/summary/daily`
   - Goals Service: `http://gamification-service/api/summary/daily`
   - Learning Service: `http://learning-service/api/summary/daily`
4. Dữ liệu và lịch sử được đưa vào context cho AI
5. AI xử lý và trả về JSON response
6. Response được sanitize (loại bỏ thông tin nhạy cảm như số thẻ tín dụng, SSN, từ ngữ không phù hợp)
7. Message được lưu vào database (trừ trường hợp gọi với `context` thuộc nhóm widget, xem bên dưới)

##### Chế độ widget (`context`)
- Dùng để render 3 thẻ nhỏ trong màn Cá nhân mà không muốn lưu lịch sử.
- Giá trị hợp lệ: `SPENDING_WIDGET`, `SAVING_WIDGET`, `GOAL_WIDGET`.
- Khi gửi `context`, có thể để trống `question`; backend sẽ tự tạo prompt theo preset và **không lưu** kết quả vào `chat_history`.
- Mỗi widget nên dùng conversationId cố định riêng (hoặc bỏ trống) vì lịch sử không được lưu.

**Timeout:**
- Downstream services: 5 giây
- Gemini API: 60 giây (có thể cấu hình)

**Example Request - Tạo conversation mới:**
```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Tôi nên tiết kiệm bao nhiêu mỗi tháng?"
  }'
```

**Example Response:**
```json
{
  "userId": "user123",
  "question": "Tôi nên tiết kiệm bao nhiêu mỗi tháng?",
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "answer": "Chào bạn! Tôi là cố vấn tài chính thông minh của EduFinAI. Rất vui được hỗ trợ bạn hôm nay. Dựa trên thu nhập của bạn, nên tiết kiệm ít nhất 20% mỗi tháng...",
  "tips": [
    "Để tôi có thể hỗ trợ tốt nhất, hãy bắt đầu bằng cách chia sẻ mục tiêu tài chính của bạn",
    "Bạn có thể đặt câu hỏi về bất kỳ chủ đề nào từ quản lý chi tiêu cá nhân đến các chiến lược đầu tư phức tạp"
  ],
  "disclaimers": [
    "Thông tin và lời khuyên được cung cấp bởi EduFinAI chỉ mang tính chất tham khảo và giáo dục chung",
    "Quyết định tài chính cuối cùng thuộc về bạn và bạn nên tham khảo ý kiến của chuyên gia tài chính có giấy phép"
  ],
  "model": "gemini-2.5-flash",
  "promptTokens": 150,
  "completionTokens": 200,
  "totalTokens": 350,
  "createdAt": "2024-01-15T10:30:00+07:00"
}
```

**Example Request - Tiếp tục conversation:**
```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "550e8400-e29b-41d4-a716-446655440000",
    "question": "Làm thế nào để tăng tiết kiệm lên 30%?"
  }'
```

**Example Request - Widget (không lưu lịch sử):**
```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "advisor-spending",
    "context": "SPENDING_WIDGET"
  }'
```

**Error Responses:**

**400 Bad Request** - Validation Error
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Question cannot be blank unless context preset is provided",
  "timestamp": "2024-01-15T10:30:00+07:00"
}
```

**500 Internal Server Error** - Gemini API Error
```json
{
  "code": "GEMINI_CALL_FAILED",
  "message": "Gemini API error [HTTP_503]: Service unavailable",
  "timestamp": "2024-01-15T10:30:00+07:00"
}
```

**Checklist triển khai frontend:**
- Lần đầu chat: gọi `/ai/chat/ask` chỉ với `question`, lưu `conversationId` trả về.
- Các lần sau: gửi cùng `conversationId` để giữ lịch sử.
- Các widget Tư vấn AI: gửi `context` tương ứng, có thể bỏ `conversationId` hoặc đặt cố định (`advisor-spending`...), backend sẽ không lưu lịch sử nên danh sách hội thoại không thay đổi.
- Nếu backend trả lỗi 400 vì thiếu câu hỏi, hãy kiểm tra đã gửi `context` chưa.

---

#### 1.2. Get User Conversations

Lấy danh sách tất cả conversations của một user, sắp xếp theo thời gian cập nhật mới nhất.

**Endpoint:** `GET /api/chat/conversations`
> User ID được lấy từ JWT, không cần query param.

**Request Headers:**
```
Authorization: Bearer <JWT>
Accept: application/json
```

**Response:** `200 OK`

**Response Body:**
```json
[
  {
    "conversationId": "string",
    "userId": "string",
    "title": "string",
    "messageCount": 0,
    "createdAt": "2024-01-15T10:00:00+07:00",
    "updatedAt": "2024-01-15T10:30:00+07:00",
    "relativeTime": "2 phút trước"
  }
]
```

**Response Schema:**
| Field | Type | Description |
|-------|------|-------------|
| `conversationId` | string | ID của conversation |
| `userId` | string | ID của user |
| `title` | string | Tiêu đề conversation (câu hỏi đầu tiên, tối đa 100 ký tự) |
| `messageCount` | integer | Số lượng messages trong conversation |
| `createdAt` | string (ISO 8601) | Thời gian tạo conversation |
| `updatedAt` | string (ISO 8601) | Thời gian message cuối cùng |
| `relativeTime` | string | Thời gian tương đối (ví dụ: "Vừa xong", "2 phút trước", "1 giờ trước", "Hôm qua", "3 ngày trước", "2 tuần trước", "5 tháng trước", hoặc "dd/MM/yyyy" nếu quá 1 năm) |

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/chat/conversations" \
  -H "Authorization: Bearer <JWT>"
```

**Example Response:**
```json
[
  {
    "conversationId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user123",
    "title": "Tôi nên tiết kiệm bao nhiêu mỗi tháng?",
    "messageCount": 5,
    "createdAt": "2024-01-15T10:00:00+07:00",
    "updatedAt": "2024-01-15T10:30:00+07:00",
    "relativeTime": "2 phút trước"
  },
  {
    "conversationId": "660e8400-e29b-41d4-a716-446655440001",
    "userId": "user123",
    "title": "Cách đầu tư cho người mới bắt đầu?",
    "messageCount": 3,
    "createdAt": "2024-01-14T09:00:00+07:00",
    "updatedAt": "2024-01-14T09:15:00+07:00",
    "relativeTime": "Hôm qua"
  }
]
```

---

#### 1.3. Get Conversation History

Lấy toàn bộ lịch sử messages của một conversation cụ thể.

**Endpoint:** `GET /api/chat/conversations/{conversationId}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `conversationId` | string | Yes | ID của conversation cần lấy lịch sử |

**Request Headers:**
```
Authorization: Bearer <JWT>
Accept: application/json
```

**Response:** `200 OK`

**Response Body:**
```json
{
  "conversationId": "string",
  "userId": "string",
  "messages": [
    {
      "id": 0,
      "conversationId": "string",
      "userId": "string",
      "question": "string",
      "answer": "string",
      "tips": ["string"],
      "disclaimers": ["string"],
      "model": "string",
      "promptTokens": 0,
      "completionTokens": 0,
      "totalTokens": 0,
      "createdAt": "2024-01-15T10:00:00+07:00"
    }
  ]
}
```

**Response Schema:**
| Field | Type | Description |
|-------|------|-------------|
| `conversationId` | string | ID của conversation |
| `userId` | string | ID của user |
| `messages` | array[ChatMessage] | Danh sách messages, sắp xếp theo thời gian tạo (từ cũ đến mới) |

**ChatMessage Schema:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | integer | ID của message |
| `conversationId` | string | ID của conversation |
| `userId` | string | ID của user |
| `question` | string | Câu hỏi của user |
| `answer` | string | Câu trả lời từ AI |
| `tips` | array[string] | Danh sách tips |
| `disclaimers` | array[string] | Danh sách disclaimers |
| `model` | string | Model Gemini được sử dụng |
| `promptTokens` | integer | Số token trong prompt |
| `completionTokens` | integer | Số token trong response |
| `totalTokens` | integer | Tổng số token |
| `createdAt` | string (ISO 8601) | Thời gian tạo message |

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/chat/conversations/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <JWT>"
```

**Example Response:**
```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user123",
  "messages": [
    {
      "id": 1,
      "conversationId": "550e8400-e29b-41d4-a716-446655440000",
      "userId": "user123",
      "question": "Tôi nên tiết kiệm bao nhiêu mỗi tháng?",
      "answer": "Chào bạn! Tôi là cố vấn tài chính thông minh của EduFinAI...",
      "tips": ["Mẹo 1", "Mẹo 2"],
      "disclaimers": ["Lưu ý 1"],
      "model": "gemini-2.5-flash",
      "promptTokens": 150,
      "completionTokens": 200,
      "totalTokens": 350,
      "createdAt": "2024-01-15T10:00:00+07:00"
    },
    {
      "id": 2,
      "conversationId": "550e8400-e29b-41d4-a716-446655440000",
      "userId": "user123",
      "question": "Làm thế nào để tăng tiết kiệm lên 30%?",
      "answer": "Để tăng tiết kiệm lên 30%, bạn có thể...",
      "tips": ["Mẹo 1"],
      "disclaimers": ["Lưu ý 1"],
      "model": "gemini-2.5-flash",
      "promptTokens": 200,
      "completionTokens": 250,
      "totalTokens": 450,
      "createdAt": "2024-01-15T10:15:00+07:00"
    }
  ]
}
```

**Error Responses:**

**404 Not Found** - Conversation không tồn tại
```json
{
  "code": "404",
  "message": "Conversation not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00+07:00"
}
```

---

#### 1.4. Delete Conversation

Xóa một conversation (xóa tất cả messages trong conversation đó).

**Endpoint:** `DELETE /api/chat/conversations/{conversationId}`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `conversationId` | string | Yes | ID của conversation cần xóa |

**Request Headers:**
```
Authorization: Bearer <JWT>
Accept: application/json
```

**Response:** `200 OK`

**Response Body:**
```json
{
  "success": true,
  "message": "Conversation deleted successfully",
  "conversationId": "string"
}
```

**Response Schema:**
| Field | Type | Description |
|-------|------|-------------|
| `success` | boolean | true nếu xóa thành công |
| `message` | string | Thông báo kết quả |
| `conversationId` | string | ID của conversation đã xóa |

**Example Request:**
```bash
curl -X DELETE "http://localhost:8080/api/chat/conversations/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <JWT>"
```

**Example Response:**
```json
{
  "success": true,
  "message": "Conversation deleted successfully",
  "conversationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Error Responses:**

**404 Not Found** - Conversation không tồn tại
```json
{
  "code": "404",
  "message": "Conversation not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00+07:00"
}
```

**Lưu ý:**
- Xóa conversation sẽ xóa **TẤT CẢ** messages trong conversation đó
- Hành động này **KHÔNG THỂ HOÀN TÁC**
- Sau khi xóa, conversation sẽ không còn xuất hiện trong danh sách conversations

---

### 2. Report API

#### 2.1. Get Daily Report

Lấy báo cáo tóm tắt tài chính & học tập theo ngày.

**Endpoint:** `GET /api/reports/daily`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `date` | string (ISO 8601 Date) | No | Ngày cần lấy report (format: `yyyy-MM-dd`). Nếu không có, mặc định là hôm nay |

**Request Headers:**
```
Authorization: Bearer <JWT>
Accept: application/json
```

**Response:** `200 OK`

**Response Body:**
```json
{
  "reportDate": "2024-01-15T00:00:00+07:00",
  "model": "gemini-2.5-flash",
  "rawSummary": "string",
  "sanitizedSummary": "string",
  "insight": "string",
  "rootCause": "string",
  "priorityAction": "string",
  "usagePromptTokens": 0,
  "usageCompletionTokens": 0,
  "usageTotalTokens": 0,
  "createdAt": "2024-01-15T02:15:00+07:00",
  "updatedAt": "2024-01-15T02:15:00+07:00"
}
```

**Response Schema:**
| Field | Type | Description |
|-------|------|-------------|
| `reportDate` | string (ISO 8601) | Ngày của report |
| `model` | string | Model Gemini được sử dụng |
| `rawSummary` | string | Báo cáo gốc từ AI (JSON chuỗi) |
| `sanitizedSummary` | string | Báo cáo đã sanitize (JSON chuỗi) |
| `insight` | string | Câu tóm tắt ngắn gọn (đã parse sẵn từ `sanitizedSummary`) |
| `rootCause` | string | Nguyên nhân chính |
| `priorityAction` | string | Hành động ưu tiên |
| `usagePromptTokens` | integer | Số token trong prompt |
| `usageCompletionTokens` | integer | Số token trong response |
| `usageTotalTokens` | integer | Tổng số token sử dụng |
| `createdAt` | string (ISO 8601) | Thời điểm tạo report |
| `updatedAt` | string (ISO 8601) | Thời điểm cập nhật report |

**Format mới của `sanitizedSummary`:**
```json
{
  "insight": "Chuỗi thông điệp 120 ký tự",
  "rootCause": "Giải thích vì sao insight xảy ra",
  "priorityAction": "Việc quan trọng nhất nên làm"
}
```
> API đã parse sẵn các trường này ra ngoài nên frontend chỉ cần dùng `insight/rootCause/priorityAction` cho ô “Báo cáo hôm nay”.

**Example Request:**
```bash
# Lấy report hôm nay
curl -X GET "http://localhost:8080/api/reports/daily"

# Lấy report theo ngày cụ thể
curl -X GET "http://localhost:8080/api/reports/daily?date=2024-01-15"
```

**Example Response:**
```json
{
  "reportDate": "2024-01-15T00:00:00+07:00",
  "model": "gemini-2.5-flash",
  "rawSummary": "{\"insight\":\"Dòng tiền dương 5.2M VND\",\"rootCause\":\"Thu nhập 8M cao hơn chi 2.8M\",\"priorityAction\":\"Dành thêm 500k vào mục tiêu laptop\"}",
  "sanitizedSummary": "{\"insight\":\"Dòng tiền dương 5.2M VND\",\"rootCause\":\"Thu nhập 8M cao hơn chi 2.8M\",\"priorityAction\":\"Dành thêm 500k vào mục tiêu laptop\"}",
  "insight": "Dòng tiền dương 5.2M VND",
  "rootCause": "Thu nhập 8M cao hơn chi 2.8M",
  "priorityAction": "Dành thêm 500k vào mục tiêu laptop",
  "usagePromptTokens": 500,
  "usageCompletionTokens": 300,
  "usageTotalTokens": 800,
  "createdAt": "2024-01-15T02:15:00+07:00",
  "updatedAt": "2024-01-15T02:15:00+07:00"
}
```

**Error Responses:**

**404 Not Found** - Report không tồn tại
```json
{
  "code": "404",
  "message": "Report not found for date: 2024-01-15",
  "timestamp": "2024-01-15T10:30:00+07:00"
}
```

---

#### 2.2. Luồng sử dụng trên trang Home (Báo cáo hôm nay)

- Frontend (module Trang chủ → ô “Báo cáo hôm nay”) gọi `GET /ai/reports/daily` qua Gateway mỗi khi người dùng mở app hoặc bấm “Tạo báo cáo mới”.
- Các field sử dụng:
  - `insight`: hiển thị dòng tiêu đề (ví dụ “Dòng tiền dương 5.2M VND”).
  - `rootCause`: câu giải thích ngắn (ví dụ “Thu nhập 8M cao hơn chi 2.8M”).
  - `priorityAction`: hành động gợi ý duy nhất (ví dụ “Dành thêm 500k vào mục tiêu laptop”).
- Nếu bất kỳ field nào trống/null, frontend nên hiển thị fallback “Chưa đủ dữ liệu để tạo báo cáo hôm nay” và gợi ý đồng bộ giao dịch.
- Khi người dùng bấm “Xem chi tiết”, có thể hiển thị lại `sanitizedSummary` (định dạng JSON chuỗi) hoặc gọi lại API để lấy dữ liệu mới nhất.
- Lưu ý: phải gửi kèm header `Authorization: Bearer <JWT>` giống các API khác.

---


## Error Handling

API sử dụng `GlobalExceptionHandler` để xử lý lỗi thống nhất.

### Error Response Format

Tất cả lỗi đều trả về format sau:

```json
{
  "code": "string",
  "message": "string",
  "timestamp": "2024-01-15T10:30:00+07:00"
}
```

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Lỗi validation (thiếu field bắt buộc, format sai) |
| `404` | 404 | Resource không tìm thấy |
| `500` | 500 | Lỗi server (Gemini API, database, etc.) |
| `INTERNAL_ERROR` | 500 | Lỗi nội bộ không xác định |
| `GEMINI_CALL_FAILED` | 500 | Lỗi khi gọi Gemini API |
| `API_KEY_MISSING` | 500 | Gemini API key chưa được cấu hình |
| `EMPTY_RESPONSE` | 500 | Gemini API trả về response rỗng |

### Common Error Scenarios

**1. Validation Error (400)**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Question cannot be blank",
  "timestamp": "2024-01-15T10:30:00+07:00"
}
```

**2. Resource Not Found (404)**
```json
{
  "code": "404",
  "message": "Conversation not found: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00+07:00"
}
```

**3. Gemini API Error (500)**
```json
{
  "code": "GEMINI_CALL_FAILED",
  "message": "Gemini API error [HTTP_503]: Service unavailable",
  "timestamp": "2024-01-15T10:30:00+07:00"
}
```

**4. Internal Server Error (500)**
```json
{
  "code": "INTERNAL_ERROR",
  "message": "Internal server error",
  "timestamp": "2024-01-15T10:30:00+07:00"
}
```

---

## Conversation History Feature

API hỗ trợ tính năng lưu lịch sử conversation giống ChatGPT:

### Cách sử dụng:

1. **Tạo conversation mới:**
   - Gửi request không có `conversationId`
   - API sẽ tạo conversation ID mới và trả về trong response
   - Lưu `conversationId` để tiếp tục cuộc hội thoại

2. **Tiếp tục conversation:**
   - Gửi request với `conversationId` từ response trước
   - API sẽ lấy lịch sử (10 messages gần nhất) và đưa vào context
   - AI sẽ có context để trả lời phù hợp hơn

3. **Xem danh sách conversations:**
   - Gọi `GET /api/chat/conversations`
   - User ID được lấy từ JWT, trả về danh sách conversation của chính người gửi request

4. **Xem lịch sử conversation:**
   - Gọi `GET /api/chat/conversations/{conversationId}`
   - Nhận toàn bộ messages trong conversation
5. **Widget presets:**
   - Khi gửi `context = SPENDING_WIDGET/SAVING_WIDGET/GOAL_WIDGET`, conversation sẽ **không** được lưu vào lịch sử để tránh “rác” danh sách.

### Lưu ý:

- Mỗi conversation được nhóm bởi `conversationId`
- Messages được lưu tự động sau mỗi response (async)
- AI sử dụng 10 messages gần nhất làm context
- Conversation được tạo tự động khi không có `conversationId`

---

## Data Sanitization

API tự động sanitize output từ AI để loại bỏ thông tin nhạy cảm:

### Các pattern bị mask:

1. **Credit Card Numbers** (13-19 chữ số)
   - Pattern: `\b\d{13,19}\b`
   - Replaced with: `####-MASKED`

2. **SSN** (9 chữ số)
   - Pattern: `\b\d{9}\b`
   - Replaced with: `###-MASKED`

3. **Profanity** (từ ngữ không phù hợp)
   - Words: "đm", "dm", "cc", "shit", "fuck"
   - Replaced with: `***`

### Sanitization Flags

Response có thể chứa flags nếu có thông tin bị mask:
- `possible_cc_masked`: Có số thẻ tín dụng bị mask
- `possible_ssn_masked`: Có SSN bị mask
- `profanity_masked`: Có từ ngữ không phù hợp bị mask

---

## Downstream Services

API tự động fetch dữ liệu từ các downstream services (nếu có):

| Service | URL | Description |
|---------|-----|-------------|
| Transaction | `http://finance-service/api/summary/daily` | Dữ liệu giao dịch |
| User Profile | `http://auth-service/api/summary/daily` | Hồ sơ người dùng |
| Goals | `http://gamification-service/api/summary/daily` | Mục tiêu tài chính |
| Learning | `http://learning-service/api/summary/daily` | Hoạt động học tập |

**Lưu ý:**
- Nếu service không available, API sẽ bỏ qua và tiếp tục với dữ liệu có sẵn
- Timeout cho mỗi service: 5 giây
- Dữ liệu được merge vào context cho AI

---

## Rate Limiting

Hiện tại API không có rate limiting. Tuy nhiên, cần lưu ý:
- Gemini API có giới hạn rate limit riêng
- Database connection pool có giới hạn
- Nên implement rate limiting cho production

---


## Testing

### Test Chat API - Tạo conversation mới

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Tôi nên đầu tư như thế nào?"
  }'
```

### Test Chat API - Tiếp tục conversation

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "550e8400-e29b-41d4-a716-446655440000",
    "question": "Có rủi ro gì không?"
  }'
```

### Test Get Conversations

```bash
curl -X GET "http://localhost:8080/api/chat/conversations" \
  -H "Authorization: Bearer <JWT>"
```

### Test Get Conversation History

```bash
curl -X GET "http://localhost:8080/api/chat/conversations/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <JWT>"
```

### Test Delete Conversation

```bash
curl -X DELETE "http://localhost:8080/api/chat/conversations/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <JWT>"
```

### Test Get Daily Report

```bash
# Lấy report hôm nay
curl -X GET "http://localhost:8080/api/reports/daily" \
  -H "Authorization: Bearer <JWT>"

# Lấy report theo ngày
curl -X GET "http://localhost:8080/api/reports/daily?date=2024-01-15" \
  -H "Authorization: Bearer <JWT>"
```

## Configuration

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GEMINI_API_KEY` | Yes | API key cho Google Gemini API |

### Application Configuration

File: `src/main/resources/application.yaml`

**Key configurations:**
- Database connection
- Gemini API URL và model
- Downstream services URLs
- CORS allowed origins
- Scheduler cron expression
- Timeout settings

---

## Database Schema

### Table: `ai_logs`

Lưu trữ lịch sử chat conversations:

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `conversation_id` | VARCHAR(64) | ID của conversation |
| `user_id` | VARCHAR(64) | ID của user |
| `question` | LONGTEXT | Câu hỏi của user |
| `prompt` | LONGTEXT | Prompt đã gửi đến Gemini |
| `model` | VARCHAR(100) | Model Gemini được sử dụng |
| `raw_answer` | LONGTEXT | Response gốc từ Gemini |
| `sanitized_answer` | LONGTEXT | Response đã sanitize (JSON) |
| `formatted_answer` | LONGTEXT | Answer đã format (text) |
| `usage_prompt_tokens` | INT | Số token trong prompt |
| `usage_completion_tokens` | INT | Số token trong response |
| `usage_total_tokens` | INT | Tổng số token |
| `created_at` | TIMESTAMP | Thời gian tạo |

**Indexes:**
- `idx_ai_logs_user_time`: (user_id, created_at)
- `idx_ai_logs_conversation`: (conversation_id, created_at)

### Table: `ai_reports`

Lưu trữ daily reports:

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `report_date` | DATE | Ngày của report (UNIQUE) |
| `model` | VARCHAR(100) | Model Gemini được sử dụng |
| `prompt` | LONGTEXT | Prompt đã gửi đến Gemini |
| `raw_summary` | LONGTEXT | Summary gốc từ Gemini |
| `sanitized_summary` | LONGTEXT | Summary đã sanitize |
| `usage_prompt_tokens` | INT | Số token trong prompt |
| `usage_completion_tokens` | INT | Số token trong response |
| `usage_total_tokens` | INT | Tổng số token |
| `metadata_json` | LONGTEXT | Metadata (JSON) |
| `created_at` | TIMESTAMP | Thời gian tạo |
| `updated_at` | TIMESTAMP | Thời gian cập nhật |

---

## Notes

1. **Time Zone:** API sử dụng **UTC timezone** cho tất cả ZonedDateTime để đảm bảo nhất quán. Tất cả timestamps trong response đều được serialize theo ISO 8601 format với UTC timezone.
2. **Database:** Sử dụng MySQL với schema được định nghĩa trong `schema.sql`
3. **Reactive:** API sử dụng Spring WebFlux (reactive), tất cả endpoints trả về `Mono<T>` hoặc `Flux<T>`
4. **Logging:** Logs được ghi vào `logs/ai-service.log`
5. **Model:** Mặc định sử dụng `gemini-2.5-flash` (có thể cấu hình)
6. **Conversation History:** Messages được lưu **synchronously** (sử dụng `thenReturn`) để đảm bảo conversation xuất hiện ngay trong danh sách sau khi gửi message
7. **Context Window:** AI sử dụng 10 messages gần nhất làm context (có thể cấu hình)
8. **Relative Time:** API tự động tính và trả về `relativeTime` (thời gian tương đối) cho mỗi conversation trong danh sách, giúp người dùng dễ dàng biết conversation nào mới nhất
9. **Transaction Management:** Delete operations sử dụng `TransactionTemplate` để đảm bảo transaction safety trong reactive context

---

## Changelog

### Version 0.0.4-SNAPSHOT
- ✅ Bắt buộc JWT cho toàn bộ API, cập nhật tài liệu header & ví dụ
- ✅ Thêm `context` cho `/api/chat/ask`, mô tả chế độ widget và bỏ lưu lịch sử khi dùng preset
- ✅ Report API parse sẵn `insight/rootCause/priorityAction`, cập nhật response schema
- ✅ Gỡ endpoint/scheduler cũ `POST /api/reports/daily/generate` khỏi tài liệu
- ✅ Cập nhật Postman/tài liệu hướng dẫn frontend

### Version 0.0.3-SNAPSHOT
- ✅ Thêm tính năng hiển thị thời gian tương đối (relativeTime) cho conversations
- ✅ Cải thiện transaction management cho delete operations
- ✅ Standardize timezone handling (UTC) cho tất cả timestamps
- ✅ Cải thiện error handling và logging

### Version 0.0.2-SNAPSHOT
- ✅ Thêm tính năng Conversation History (giống ChatGPT)
- ✅ Thêm endpoints để quản lý conversations (GET, DELETE)
- ✅ Cập nhật Chat API để hỗ trợ conversationId
- ✅ Cải thiện response format (answer, tips, disclaimers thay vì JSON string)
- ✅ AI tự động sử dụng context từ conversation history (10 messages gần nhất)
- ✅ Thêm tính năng xóa conversation

### Version 0.0.1-SNAPSHOT
- Initial release
- Chat API với Gemini integration
- Daily Report API
- Data sanitization
- Downstream services integration
- Scheduler cho daily reports

---

## Support

Nếu có vấn đề, vui lòng kiểm tra:
1. Logs trong `logs/ai-service.log`
2. Gemini API key đã được cấu hình
3. Database connection
4. Downstream services đang chạy (nếu cần)
5. Database schema đã được cập nhật với `conversation_id` và `formatted_answer`

---

**Last Updated:** 2024-11-16

---

## API Summary

### Chat API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/chat/ask` | Gửi câu hỏi và nhận phản hồi từ AI |
| `GET` | `/api/chat/conversations` | Lấy danh sách conversations của user |
| `GET` | `/api/chat/conversations/{conversationId}` | Lấy lịch sử của một conversation |
| `DELETE` | `/api/chat/conversations/{conversationId}` | Xóa một conversation |

### Report API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/reports/daily` | Lấy daily report theo ngày (on-demand) |

### Total: 5 API Endpoints
