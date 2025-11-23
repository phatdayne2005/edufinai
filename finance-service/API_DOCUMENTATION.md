# Finance Service API Documentation

## 📋 Mục lục

1. [Tổng quan](#tổng-quan)
2. [Authentication](#authentication)
3. [CORS Configuration](#cors-configuration)
4. [Endpoints](#endpoints)
   - [Balance Management](#1-balance-management-quản-lý-số-dư)
   - [Transaction Management](#2-transaction-management-quản-lý-giao-dịch)
   - [Category Management](#3-category-management-quản-lý-danh-mục)
   - [Goal Management](#4-goal-management-quản-lý-mục-tiêu-tài-chính)
   - [Summary](#5-summary-tổng-hợp-tài-chính)
5. [Data Models](#data-models)
6. [Enums](#enums)
7. [Error Handling](#error-handling)
8. [Examples](#examples)
9. [Configuration](#configuration)

---

## Tổng quan

Finance Service là một microservice trong hệ thống EduFinAI, chịu trách nhiệm quản lý:
- **Số dư tài chính** (Balance Management)
- **Giao dịch tài chính** (Thu nhập, Chi tiêu và Rút tiền từ goal)
- **Danh mục** (Categories)
- **Mục tiêu tài chính** (Financial Goals)
- **Tổng hợp tài chính** (Financial Summary)

### ⚠️ QUAN TRỌNG: Gateway Routing

**Frontend PHẢI gọi API thông qua Gateway, KHÔNG gọi trực tiếp vào service.**

**Gateway Base URL:** `http://localhost:8080`  
**Gateway Port:** 8080  
**Service Base URL (chỉ dùng cho testing/internal):** `http://localhost:8202`  
**Service Port:** 8202  
**Service Name:** finance-service  
**Eureka Registration:** `http://localhost:8761/eureka`

### Gateway Routing Configuration

Gateway được cấu hình để route các request từ frontend đến finance-service:

**Route Pattern:**
- **Frontend gọi:** `/finance/**`
- **Gateway rewrite:** `/finance/?(?<segment>.*)` → `/api/${segment}`
- **Service nhận:** `/api/**`

**Ví dụ:**
- Frontend gọi: `GET http://localhost:8080/finance/v1/balance`
- Gateway rewrite: `GET http://localhost:8202/api/v1/balance` (internal routing)
- Service xử lý: `/api/v1/balance`

**Lưu ý quan trọng:**
- ✅ **ĐÚNG:** Frontend gọi `/finance/v1/balance` (không có `/api/` trong path)
- ❌ **SAI:** Frontend gọi `/finance/api/v1/balance` (sẽ bị rewrite thành `/api/api/v1/balance`)

**Tất cả các endpoint examples trong document này đã được cập nhật để sử dụng Gateway URL.**

---

## Authentication

Service sử dụng **JWT (JSON Web Token)** authentication. Tất cả các endpoints (trừ public endpoints) yêu cầu JWT token hợp lệ.

### JWT Token Format

**Header:**
```
Authorization: Bearer <jwt-token>
```

**Token Requirements:**
- Token phải được tạo bởi auth-service với cùng secret key
- Token phải có `subject` (sub) claim chứa UUID của user
- Token phải chưa hết hạn

### Public Endpoints (Không cần authentication)

Các endpoints sau không yêu cầu JWT token:
- `/actuator/**` - Spring Boot Actuator endpoints
- `/v3/api-docs/**` - OpenAPI documentation
- `/swagger-ui/**` - Swagger UI
- `POST /api/v1/auth/**` - Authentication endpoints (nếu có)

### Protected Endpoints

Tất cả các endpoints khác yêu cầu JWT token hợp lệ trong header.

**Example Request (qua Gateway):**
```bash
curl -X GET http://localhost:8080/finance/v1/transactions \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Example Request (trực tiếp service - chỉ dùng cho testing/internal):**
```bash
curl -X GET http://localhost:8202/api/v1/transactions \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## CORS Configuration

Service đã được cấu hình CORS để cho phép requests từ frontend.

**Allowed Origins:**
- `http://localhost:3000` (React default)
- `http://localhost:5173` (Vite default)

**Allowed Methods:**
- GET, POST, PUT, DELETE, PATCH, OPTIONS

**Allowed Headers:**
- Authorization
- Content-Type
- Accept

**Credentials:** Enabled

**Max Age:** 3600 seconds

Có thể cấu hình thêm origins trong `application.properties`:
```properties
app.cors.allowed-origins=http://localhost:3000,http://localhost:5173,https://yourdomain.com
```

---

## Endpoints

### 1. Balance Management (Quản lý Số dư)

#### 1.1. Khai báo số dư ban đầu

**Endpoint:** `POST /finance/v1/balance/initialize` (qua Gateway)  
**Service Endpoint:** `POST /api/v1/balance/initialize` (internal)

**Mô tả:** Khai báo số dư ban đầu của user. Chỉ có thể khai báo một lần duy nhất khi user đăng nhập lần đầu.

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "amount": 10000000  // Bắt buộc: Số dư ban đầu (BigDecimal, phải > 0)
}
```

**Response 200 OK:**
```json
{
  "userId": "user-uuid",
  "initialBalance": 10000000,
  "createdAt": "2025-01-19T10:30:00",
  "updatedAt": "2025-01-19T10:30:00"
}
```

**Validation Rules:**
- `amount`: Bắt buộc, phải là số dương (> 0)
- User chỉ có thể khai báo số dư ban đầu một lần duy nhất

**Business Logic:**
- Số dư ban đầu được lưu vào bảng `user_balance`
- Sau khi khai báo, số dư hiện tại = initialBalance + totalIncome - totalExpense - totalWithdrawal
- Nếu user đã khai báo rồi, sẽ trả về lỗi 400

**Error Responses:**

| Status Code | Mô tả | Response Body |
|-------------|-------|---------------|
| 400 | Số dư ban đầu đã được khai báo hoặc dữ liệu không hợp lệ | `{"timestamp": "...", "status": 400, "error": "Bad Request", "message": "Số dư ban đầu đã được khai báo. Không thể khai báo lại."}` |
| 401 | Unauthorized - Thiếu hoặc JWT token không hợp lệ | `{"timestamp": "...", "status": 401, "error": "Unauthorized", "message": "Full authentication is required..."}` |
| 500 | Lỗi server nội bộ | `{"timestamp": "...", "status": 500, "error": "Internal Server Error", "message": "..."}` |

---

#### 1.2. Xem số dư hiện tại

**Endpoint:** `GET /finance/v1/balance` (qua Gateway)  
**Service Endpoint:** `GET /api/v1/balance` (internal)

**Mô tả:** Lấy thông tin số dư hiện tại của user, bao gồm số dư ban đầu, tổng thu nhập, tổng chi tiêu, tổng rút tiền và số dư hiện tại.

**Authentication:** Required (JWT)

**Response 200 OK:**
```json
{
  "currentBalance": 11000000,
  "initialBalance": 10000000,
  "totalIncome": 30000000,
  "totalGoalDeposit": 5000000,
  "totalExpense": 12000000,
  "totalWithdrawal": 2000000
}
```

**Business Logic:**
- `currentBalance` = `initialBalance` + `totalIncome` - `totalExpense` - `totalGoalDeposit` + `totalWithdrawal`
  - `totalIncome`: Tổng thu nhập thông thường (INCOME không có goalId) - **cộng vào số dư**
  - `totalGoalDeposit`: Tổng nạp vào goal (INCOME có goalId) - **trừ khỏi số dư** (tiền bị khóa)
  - `totalExpense`: Tổng chi tiêu - **trừ khỏi số dư**
  - `totalWithdrawal`: Tổng rút từ goal - **cộng vào số dư** (tiền được giải phóng)
- Nếu user chưa khai báo số dư ban đầu, `initialBalance` = 0
- Tất cả tính toán chỉ dựa trên transactions có status = "ACTIVE"
- **Lưu ý quan trọng:** Khi nạp tiền vào goal, số tiền đó bị khóa và không thể sử dụng cho các giao dịch khác. Chỉ khi rút từ goal thì số tiền mới được giải phóng và cộng vào số dư.

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

#### 1.3. Kiểm tra đã khai báo số dư ban đầu

**Endpoint:** `GET /finance/v1/balance/check-initialized` (qua Gateway)  
**Service Endpoint:** `GET /api/v1/balance/check-initialized` (internal)

**Mô tả:** Kiểm tra xem user đã khai báo số dư ban đầu chưa.

**Authentication:** Required (JWT)

**Response 200 OK:**
```json
true  // hoặc false
```

**Use Case:** Frontend có thể sử dụng endpoint này để hiển thị form khai báo số dư ban đầu cho user mới.

---

### 2. Transaction Management (Quản lý Giao dịch)

#### 2.1. Tạo giao dịch mới

**Endpoint:** `POST /finance/v1/transactions` (qua Gateway)  
**Service Endpoint:** `POST /api/v1/transactions` (internal)

**Mô tả:** Tạo một giao dịch thu nhập hoặc chi tiêu mới.

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "type": "INCOME",                    // Bắt buộc: "INCOME" hoặc "EXPENSE"
  "amount": 5000000,                   // Bắt buộc: Số tiền (BigDecimal)
  "name": "Lương tháng 1",            // Bắt buộc: Tên giao dịch (String)
  "categoryId": "uuid-category-id",   // Tùy chọn: ID danh mục (UUID) - Bắt buộc khi không có goalId, tự động tạo "Tiết kiệm" khi có goalId
  "note": "Lương cơ bản",             // Tùy chọn: Ghi chú (String)
  "goalId": "uuid-goal-id",           // Tùy chọn: ID mục tiêu (UUID) - chỉ áp dụng cho INCOME
  "transactionDate": "2025-01-19T10:30:00"  // Tùy chọn: Ngày giao dịch (ISO 8601), mặc định là now()
}
```

**Response 200 OK:**
```json
{
  "transactionId": "e1f1d8a3-0000-0000-0000-000000000000",
  "type": "INCOME",
  "name": "Lương tháng 1",
  "category": "Salary",
  "note": "Lương cơ bản",
  "amount": 5000000,
  "transactionDate": "2025-01-19T10:30:00",
  "goalId": "a12b34c5-0000-0000-0000-000000000000"
}
```

**Validation Rules:**
- `type`: Bắt buộc, phải là "INCOME" hoặc "EXPENSE" (case-sensitive)
- `amount`: Bắt buộc, phải là số dương
- `name`: Bắt buộc, không được rỗng
- `categoryId`: 
  - **Bắt buộc** khi không có `goalId` (transaction thông thường)
  - **Tùy chọn** khi có `goalId` (nạp vào goal) - nếu không có, hệ thống tự động tạo/gán category "Tiết kiệm"
- `goalId`: Tùy chọn, chỉ áp dụng cho INCOME transactions
- `transactionDate`: Tùy chọn, format ISO 8601 (yyyy-MM-ddTHH:mm:ss)

**Business Logic:**
- **Validation số dư:**
  - Nếu `type` là "EXPENSE": Kiểm tra số dư hiện tại >= `amount` (nếu không đủ → 400)
  - Nếu `goalId` được cung cấp và `type` là "INCOME": Kiểm tra số dư hiện tại >= số tiền thực tế sẽ nạp
- Nếu `goalId` được cung cấp và `type` là "INCOME":
  - Kiểm tra goal chưa được xác nhận hoàn thành (nếu COMPLETED → 400)
  - Kiểm tra goal chưa đủ tiền (nếu `savedAmount >= amount` → 400)
  - Tính số tiền thực tế sẽ nạp: `actualDepositAmount = min(request.getAmount(), remainingAmount)`
    - Nếu nạp > số tiền còn lại để hoàn thành mục tiêu → chỉ nạp đủ số tiền còn lại
    - Số dư thừa không bị trừ (chỉ tạo 1 transaction với số tiền vừa đủ)
  - Transaction sẽ được gắn vào goal và `savedAmount` của goal sẽ được cập nhật tự động
  - Nếu `categoryId` không được cung cấp, hệ thống tự động tìm hoặc tạo category "Tiết kiệm" cho user
  - Số tiền nạp vào goal sẽ **bị trừ khỏi số dư hiện tại** (tiền bị khóa trong goal)
  - Goal `newStatus` sẽ được tự động check và update (set = COMPLETED nếu đạt mục tiêu, nhưng status vẫn ACTIVE)
- Nếu không có `goalId` và không có `categoryId` → lỗi 400 (Category is required)

**Error Responses:**

| Status Code | Mô tả | Response Body |
|-------------|-------|---------------|
| 400 | Dữ liệu không hợp lệ, không đủ số dư, goal đã hoàn thành, hoặc goal đã đủ tiền | `{"timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed: ..."}` hoặc `"Không đủ số dư. Số dư hiện tại: {currentBalance}"` hoặc `"Không đủ số dư để nạp vào mục tiêu. Số dư hiện tại: {currentBalance}"` hoặc `"Không thể nạp tiền vào mục tiêu đã hoàn thành"` hoặc `"Mục tiêu đã đủ tiền. Không thể nạp thêm"` |
| 401 | Unauthorized (thiếu hoặc JWT token không hợp lệ) | `{"timestamp": "...", "status": 401, "error": "Unauthorized", "message": "Full authentication is required..."}` |
| 403 | Forbidden - Không thể link transaction vào goal của user khác | `{"timestamp": "...", "status": 403, "error": "Forbidden", "message": "Cannot link transaction to other user's goal"}` |
| 404 | Category hoặc Goal không tồn tại | `{"timestamp": "...", "status": 404, "error": "Not Found", "message": "Category not found"}` hoặc `"Goal not found"` |
| 500 | Lỗi server nội bộ | `{"timestamp": "...", "status": 500, "error": "Internal Server Error", "message": "..."}` |

---

#### 2.2. Xóa giao dịch

**Endpoint:** `DELETE /finance/v1/transactions/{id}` (qua Gateway)  
**Service Endpoint:** `DELETE /api/v1/transactions/{id}` (internal)

**Mô tả:** Xóa (soft delete) một giao dịch. Chỉ user sở hữu giao dịch mới có thể xóa.

**Authentication:** Required (JWT)

**Path Parameters:**
- `id` (UUID, required): ID của giao dịch cần xóa

**Response 200 OK:**
```json
(Empty body)
```

**Business Logic:**
- Nếu transaction đã được gắn vào goal và là INCOME, `savedAmount` của goal sẽ được trừ lại
- Goal status sẽ được tự động check và update

**Error Responses:**

| Status Code | Mô tả | Response Body |
|-------------|-------|---------------|
| 401 | Unauthorized | `{"timestamp": "...", "status": 401, "error": "Unauthorized", "message": "Full authentication is required..."}` |
| 403 | Forbidden (user không sở hữu transaction này) | `{"timestamp": "...", "status": 403, "error": "Forbidden", "message": "Forbidden"}` |
| 404 | Transaction không tồn tại | `{"timestamp": "...", "status": 404, "error": "Not Found", "message": "Transaction not found"}` |
| 500 | Lỗi server nội bộ | `{"timestamp": "...", "status": 500, "error": "Internal Server Error", "message": "..."}` |

---

#### 2.3. Lấy danh sách giao dịch gần đây

**Endpoint:** `GET /finance/v1/transactions/recent` (qua Gateway)  
**Service Endpoint:** `GET /api/v1/transactions/recent` (internal)

**Mô tả:** Lấy danh sách các giao dịch gần đây nhất của user, sắp xếp theo ngày giao dịch (mới nhất trước).

**Authentication:** Required (JWT)

**Query Parameters:**
- `limit` (int, optional): Số lượng giao dịch (mặc định: 5)

**Response 200 OK:**
```json
[
  {
    "transactionId": "e1f1d8a3-0000-0000-0000-000000000000",
    "type": "INCOME",
    "name": "Lương tháng 1",
    "category": "Salary",
    "note": "Lương cơ bản",
    "amount": 5000000,
    "transactionDate": "2025-01-19T10:30:00",
    "goalId": null
  },
  {
    "transactionId": "f2g2h9b4-0000-0000-0000-000000000001",
    "type": "EXPENSE",
    "name": "Mua sắm",
    "category": "Shopping",
    "note": "Mua quần áo",
    "amount": 500000,
    "transactionDate": "2025-01-18T15:20:00",
    "goalId": null
  }
]
```

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

#### 2.4. Lấy danh sách giao dịch (có phân trang)

**Endpoint:** `GET /finance/v1/transactions` (qua Gateway)  
**Service Endpoint:** `GET /api/v1/transactions` (internal)

**Mô tả:** Lấy danh sách giao dịch với phân trang và lọc theo khoảng thời gian.

**Authentication:** Required (JWT)

**Query Parameters:**
- `page` (int, optional): Số trang (bắt đầu từ 0, mặc định: 0)
- `size` (int, optional): Số lượng items mỗi trang (mặc định: 15)
- `startDate` (LocalDateTime, optional): Ngày bắt đầu (ISO 8601 format)
- `endDate` (LocalDateTime, optional): Ngày kết thúc (ISO 8601 format)

**Note:** Nếu không cung cấp `startDate` hoặc `endDate`, mặc định sẽ lấy tháng hiện tại.

**Response 200 OK:**
```json
{
  "content": [
    {
      "transactionId": "e1f1d8a3-0000-0000-0000-000000000000",
      "type": "INCOME",
      "name": "Lương tháng 1",
      "category": "Salary",
      "note": "Lương cơ bản",
      "amount": 5000000,
      "transactionDate": "2025-01-19T10:30:00",
      "goalId": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 15
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 1
}
```

**Example Request (qua Gateway):**
```bash
GET /finance/v1/transactions?page=0&size=20&startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59
```

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

### 3. Category Management (Quản lý Danh mục)

#### 3.1. Lấy danh sách danh mục

**Endpoint:** `GET /finance/v1/categories` (qua Gateway)  
**Service Endpoint:** `GET /api/v1/categories` (internal)

**Mô tả:** Lấy danh sách tất cả các danh mục của user hiện tại (bao gồm cả default categories).

**Authentication:** Required (JWT)

**Response 200 OK:**
```json
[
  {
    "categoryId": "c1d2e3f4-0000-0000-0000-000000000000",
    "userId": "user-uuid",
    "name": "Salary",
    "type": "INCOME",
    "isDefault": false,
    "createdAt": "2025-01-01T00:00:00"
  },
  {
    "categoryId": "d2e3f4g5-0000-0000-0000-000000000001",
    "userId": "user-uuid",
    "name": "Shopping",
    "type": "EXPENSE",
    "isDefault": false,
    "createdAt": "2025-01-01T00:00:00"
  },
  {
    "categoryId": "e3f4g5h6-0000-0000-0000-000000000002",
    "userId": "00000000-0000-0000-0000-000000000000",
    "name": "Khác",
    "type": "BOTH",
    "isDefault": true,
    "createdAt": "2025-01-01T00:00:00"
  }
]
```

**Lưu ý:**
- Category "Khác" là default category (type = BOTH), luôn tồn tại và không thể xóa
- Category "Khác" có thể dùng cho cả INCOME và EXPENSE transactions

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

#### 3.2. Tạo danh mục mới

**Endpoint:** `POST /finance/v1/categories` (qua Gateway)  
**Service Endpoint:** `POST /api/v1/categories` (internal)

**Mô tả:** Tạo một danh mục mới cho user.

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "name": "Entertainment",  // Bắt buộc: Tên danh mục (String, không được rỗng)
  "type": "EXPENSE"        // Tùy chọn: Loại danh mục (INCOME, EXPENSE, BOTH), mặc định: EXPENSE
}
```

**Response 200 OK:**
```json
{
  "categoryId": "e3f4g5h6-0000-0000-0000-000000000002",
  "userId": "user-uuid",
  "name": "Entertainment",
  "type": "EXPENSE",
  "isDefault": false,
  "createdAt": "2025-01-19T10:30:00"
}
```

**Validation Rules:**
- `name`: Bắt buộc, không được rỗng (NotBlank)
- `type`: Tùy chọn, phải là một trong: `INCOME`, `EXPENSE`, `BOTH` (mặc định: `EXPENSE`)
- Tên danh mục phải unique cho mỗi user (unique constraint: user_id + name)

**Category Type:**
- `INCOME`: Chỉ dùng cho khoản thu (INCOME transactions)
- `EXPENSE`: Chỉ dùng cho khoản chi (EXPENSE transactions)
- `BOTH`: Dùng cho cả khoản thu và khoản chi (INCOME và EXPENSE transactions)

**Error Responses:**

| Status Code | Mô tả | Response Body |
|-------------|-------|---------------|
| 400 | Dữ liệu không hợp lệ hoặc danh mục đã tồn tại | `{"timestamp": "...", "status": 400, "error": "Bad Request", "message": "Category already exists"}` hoặc validation error |
| 401 | Unauthorized | `{"timestamp": "...", "status": 401, "error": "Unauthorized", "message": "Full authentication is required..."}` |
| 500 | Lỗi server nội bộ | `{"timestamp": "...", "status": 500, "error": "Internal Server Error", "message": "..."}` |

---

#### 3.3. Xóa danh mục

**Endpoint:** `DELETE /finance/v1/categories/{id}` (qua Gateway)  
**Service Endpoint:** `DELETE /api/v1/categories/{id}` (internal)

**Mô tả:** Xóa một danh mục. Chỉ user sở hữu danh mục mới có thể xóa. Khi xóa category, tất cả transaction đang sử dụng category đó sẽ tự động chuyển sang category "Khác" (default category, type = BOTH).

**Authentication:** Required (JWT)

**Path Parameters:**
- `id` (UUID, required): ID của danh mục cần xóa

**Business Logic:**
1. Kiểm tra category có tồn tại không
2. Không cho phép xóa category "Khác" (default category, type = BOTH)
3. Không cho phép xóa default categories khác
4. Chỉ cho phép xóa category của chính user đó
5. Tìm tất cả transaction đang sử dụng category này
6. Update tất cả transaction sang category "Khác"
7. Xóa category

**Response 200 OK:**
```json
(Empty body)
```

**Error Responses:**

| Status Code | Mô tả | Response Body |
|-------------|-------|---------------|
| 400 | Không thể xóa default category hoặc category "Khác" | `{"timestamp": "...", "status": 400, "error": "Bad Request", "message": "Cannot delete default category 'Khác'"}` hoặc `"Cannot delete default category"` |
| 401 | Unauthorized | `{"timestamp": "...", "status": 401, "error": "Unauthorized", "message": "Full authentication is required..."}` |
| 403 | Forbidden (user không sở hữu category này) | `{"timestamp": "...", "status": 403, "error": "Forbidden", "message": "Cannot delete other user's category"}` |
| 404 | Category không tồn tại | `{"timestamp": "...", "status": 404, "error": "Not Found", "message": "Category not found"}` |
| 500 | Lỗi server nội bộ | `{"timestamp": "...", "status": 500, "error": "Internal Server Error", "message": "..."}` |

**Lưu ý:**
- Category "Khác" là default category (type = BOTH), luôn tồn tại và không thể xóa
- Khi xóa category, tất cả transaction (cả ACTIVE và DELETED) sẽ tự động chuyển sang category "Khác"
- Lịch sử transaction được giữ nguyên, chỉ category được thay đổi

---

### 4. Goal Management (Quản lý Mục tiêu Tài chính)

#### 4.1. Tạo mục tiêu mới

**Endpoint:** `POST /finance/v1/goals` (qua Gateway)  
**Service Endpoint:** `POST /api/v1/goals` (internal)

**Mô tả:** Tạo một mục tiêu tài chính mới.

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "title": "Mua laptop mới",                    // Bắt buộc: Tên mục tiêu (String)
  "amount": 15000000,                          // Bắt buộc: Số tiền mục tiêu (BigDecimal)
  "endAt": "2025-12-31T00:00:00",             // Bắt buộc: Hạn hoàn thành (ISO 8601)
  "startAt": "2025-01-01T00:00:00"            // Tùy chọn: Ngày bắt đầu (ISO 8601), mặc định là now()
}
```

**Response 200 OK:**
```json
{
  "goalId": "a12b34c5-0000-0000-0000-000000000000",
  "userId": "user-uuid",
  "title": "Mua laptop mới",
  "amount": 15000000,
  "startAt": "2025-01-19T10:30:00",
  "endAt": "2025-12-31T00:00:00",
  "status": "ACTIVE",
  "updatedAt": "2025-01-19T10:30:00",
  "newStatus": "ACTIVE",
  "savedAmount": 0
}
```

**Validation Rules:**
- `title`: Bắt buộc, không được rỗng
- `amount`: Bắt buộc, phải là số dương
- `endAt`: Bắt buộc, phải là thời gian trong tương lai
- `startAt`: Tùy chọn, nếu không có sẽ mặc định là thời gian hiện tại

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 400 | Dữ liệu không hợp lệ |
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

#### 4.2. Lấy danh sách mục tiêu

**Endpoint:** `GET /finance/v1/goals` (qua Gateway)  
**Service Endpoint:** `GET /api/v1/goals` (internal)

**Mô tả:** Lấy danh sách tất cả các mục tiêu của user. Status và newStatus sẽ được tự động check và update:
- **newStatus = COMPLETED**: Nếu `savedAmount >= amount` (nhưng status vẫn ACTIVE cho đến khi user xác nhận)
- **status = FAILED**: Nếu `endAt < now` và `savedAmount < amount`
- **status = ACTIVE**: Còn lại

**Authentication:** Required (JWT)

**Response 200 OK:**
```json
[
  {
    "goalId": "a12b34c5-0000-0000-0000-000000000000",
    "userId": "user-uuid",
    "title": "Mua laptop mới",
    "amount": 15000000,
    "startAt": "2025-01-01T00:00:00",
    "endAt": "2025-12-31T00:00:00",
    "status": "ACTIVE",
    "updatedAt": "2025-01-19T10:30:00",
    "newStatus": "COMPLETED",
    "savedAmount": 15000000
  },
  {
    "goalId": "b23c45d6-0000-0000-0000-000000000001",
    "userId": "user-uuid",
    "title": "Tiết kiệm cho kỳ nghỉ",
    "amount": 5000000,
    "startAt": "2025-01-01T00:00:00",
    "endAt": "2025-06-30T00:00:00",
    "status": "COMPLETED",
    "updatedAt": "2025-01-19T10:30:00",
    "newStatus": "COMPLETED",
    "savedAmount": 5000000
  }
]
```

**Business Logic:**
- `status` và `newStatus` được tự động check và update mỗi khi gọi endpoint này
- `savedAmount` được cập nhật tự động khi có INCOME transaction được gắn vào goal
- Khi `savedAmount >= amount`, `newStatus` sẽ được set = COMPLETED (nhưng `status` vẫn ACTIVE)
- User phải xác nhận hoàn thành thông qua API `POST /{id}/confirm-completion` để chuyển `status` sang COMPLETED
- Khi `status = COMPLETED` (đã xác nhận), goal không thể thao tác (xóa, rút, nạp) nữa

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

#### 4.3. Xác nhận hoàn thành mục tiêu

**Endpoint:** `POST /finance/v1/goals/{id}/confirm-completion` (qua Gateway)  
**Service Endpoint:** `POST /api/v1/goals/{id}/confirm-completion` (internal)

**Mô tả:** Xác nhận hoàn thành một mục tiêu tài chính. Chỉ cho phép xác nhận khi `savedAmount >= amount`. Sau khi xác nhận, goal chuyển sang trạng thái COMPLETED và không thể thao tác (xóa, rút, nạp) nữa.

**Authentication:** Required (JWT)

**Path Parameters:**
- `id` (UUID, required): ID của mục tiêu cần xác nhận hoàn thành

**Response 200 OK:**
```json
{
  "goalId": "a12b34c5-0000-0000-0000-000000000000",
  "userId": "user-uuid",
  "title": "Mua laptop mới",
  "amount": 15000000,
  "startAt": "2025-01-01T00:00:00",
  "endAt": "2025-12-31T00:00:00",
  "status": "COMPLETED",
  "updatedAt": "2025-01-19T10:30:00",
  "newStatus": "COMPLETED",
  "savedAmount": 15000000
}
```

**Validation Rules:**
- `id`: Phải là UUID hợp lệ và tồn tại trong database
- `savedAmount >= amount`: Mục tiêu phải đã đủ tiền mới được xác nhận
- Goal chưa được xác nhận hoàn thành (status != COMPLETED)
- User chỉ có thể xác nhận mục tiêu của chính mình

**Business Logic:**
1. Kiểm tra goal tồn tại và thuộc về user
2. Kiểm tra `savedAmount >= amount` (nếu không đủ → 400)
3. Kiểm tra goal chưa được xác nhận (nếu đã COMPLETED → 400)
4. Chuyển goal sang trạng thái COMPLETED
5. Sau khi xác nhận, goal không thể thao tác (xóa, rút, nạp) nữa

**Error Responses:**

| Status Code | Mô tả | Response Body |
|-------------|-------|---------------|
| 400 | Mục tiêu chưa đủ tiền hoặc đã được xác nhận | `{"timestamp": "...", "status": 400, "error": "Bad Request", "message": "Mục tiêu chưa đủ tiền. Số tiền hiện có: {savedAmount}, cần: {amount}"}` hoặc `"Mục tiêu đã được xác nhận hoàn thành"` |
| 401 | Unauthorized | `{"timestamp": "...", "status": 401, "error": "Unauthorized", "message": "Full authentication is required..."}` |
| 403 | Forbidden (user không sở hữu goal này) | `{"timestamp": "...", "status": 403, "error": "Forbidden", "message": "Forbidden"}` |
| 404 | Goal không tồn tại | `{"timestamp": "...", "status": 404, "error": "Not Found", "message": "Goal not found"}` |
| 500 | Lỗi server nội bộ | `{"timestamp": "...", "status": 500, "error": "Internal Server Error", "message": "..."}` |

**Example Request (qua Gateway):**
```bash
curl -X POST http://localhost:8080/finance/v1/goals/a12b34c5-0000-0000-0000-000000000000/confirm-completion \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Lưu ý:**
- Goal chỉ tự động chuyển sang COMPLETED khi user xác nhận (không tự động)
- Nếu chưa xác nhận, goal vẫn ở trạng thái ACTIVE (có thể xóa, rút, nạp)
- Sau khi xác nhận COMPLETED, goal không thể thao tác nữa

---

#### 4.4. Rút tiền từ mục tiêu

**Endpoint:** `POST /finance/v1/goals/{id}/withdraw` (qua Gateway)  
**Service Endpoint:** `POST /api/v1/goals/{id}/withdraw` (internal)

**Mô tả:** Rút tiền từ một mục tiêu tài chính. Khi rút tiền, số tiền sẽ được chuyển vào số dư chính và `savedAmount` của goal sẽ giảm tương ứng.

**Authentication:** Required (JWT)

**Path Parameters:**
- `id` (UUID, required): ID của mục tiêu cần rút tiền

**Request Body:**
```json
{
  "amount": 5000000,              // Bắt buộc: Số tiền muốn rút (BigDecimal, phải > 0)
  "note": "Cần gấp cho việc khẩn cấp"  // Tùy chọn: Ghi chú (String)
}
```

**Response 200 OK:**
```json
{
  "transactionId": "w1x2y3z4-0000-0000-0000-000000000000",
  "type": "WITHDRAWAL",
  "name": "Rút từ mục tiêu: Mua laptop mới",
  "category": "Rút tiền",
  "note": "Cần gấp cho việc khẩn cấp",
  "amount": 5000000,
  "transactionDate": "2025-01-19T10:30:00",
  "goalId": "a12b34c5-0000-0000-0000-000000000000"
}
```

**Validation Rules:**
- `amount`: Bắt buộc, phải là số dương và không được vượt quá `savedAmount` của goal
- `id`: Phải là UUID hợp lệ và tồn tại trong database
- User chỉ có thể rút tiền từ mục tiêu của chính mình

**Business Logic:**
1. Kiểm tra goal chưa được xác nhận hoàn thành (nếu COMPLETED → 400)
2. Kiểm tra `savedAmount` của goal >= `amount` (nếu không đủ sẽ trả về lỗi 400)
3. Tạo WITHDRAWAL transaction với:
   - type = "WITHDRAWAL"
   - name = "Rút tiền từ mục tiêu \"{goal.title}\""
   - category = "Rút tiền" (tự động tạo nếu chưa có)
   - goalId = goal id
4. Giảm `savedAmount` của goal: `savedAmount = savedAmount - amount`
5. Tự động check và update goal status (set newStatus = COMPLETED nếu đạt mục tiêu, nhưng status vẫn ACTIVE)
6. Số dư hiện tại tăng: `currentBalance += amount`

**Error Responses:**

| Status Code | Mô tả | Response Body |
|-------------|-------|---------------|
| 400 | Số tiền rút vượt quá số tiền có trong mục tiêu, goal đã hoàn thành, hoặc dữ liệu không hợp lệ | `{"timestamp": "...", "status": 400, "error": "Bad Request", "message": "Không đủ số tiền trong mục tiêu. Số tiền có thể rút: {savedAmount}"}` hoặc `"Không thể rút tiền từ mục tiêu đã hoàn thành"` |
| 401 | Unauthorized | `{"timestamp": "...", "status": 401, "error": "Unauthorized", "message": "Full authentication is required..."}` |
| 403 | Forbidden (user không sở hữu goal này) | `{"timestamp": "...", "status": 403, "error": "Forbidden", "message": "Forbidden"}` |
| 404 | Goal không tồn tại | `{"timestamp": "...", "status": 404, "error": "Not Found", "message": "Goal not found"}` |
| 500 | Lỗi server nội bộ | `{"timestamp": "...", "status": 500, "error": "Internal Server Error", "message": "..."}` |

**Example Request (qua Gateway):**
```bash
curl -X POST http://localhost:8080/finance/v1/goals/a12b34c5-0000-0000-0000-000000000000/withdraw \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000000,
    "note": "Cần gấp cho việc khẩn cấp"
  }'
```

---

#### 4.5. Xóa mục tiêu

**Endpoint:** `DELETE /finance/v1/goals/{id}` (qua Gateway)  
**Service Endpoint:** `DELETE /api/v1/goals/{id}` (internal)

**Mô tả:** Xóa một mục tiêu tài chính. Xóa tất cả transaction liên quan đến goal và xóa goal. Không cho phép xóa goal đã được xác nhận hoàn thành (COMPLETED).

**Authentication:** Required (JWT)

**Path Parameters:**
- `id` (UUID, required): ID của mục tiêu cần xóa

**Response 200 OK:**
```json
(Empty body)
```

**Business Logic:**
1. Kiểm tra goal tồn tại và thuộc về user (nếu không → 404 hoặc 403)
2. Kiểm tra goal chưa được xác nhận hoàn thành (nếu COMPLETED → 400)
3. Tìm tất cả transaction liên quan đến goal
4. Xóa tất cả transaction đó
5. Xóa goal
6. Số dư tự động đúng vì transaction đã bị xóa (không còn tính vào số dư)

**Lưu ý quan trọng:**
- Khi xóa goal, tất cả transaction liên quan sẽ bị xóa
- Số dư tự động đúng vì transaction đã bị xóa (không còn tính vào số dư)
- Không cho phép xóa goal đã được xác nhận hoàn thành (COMPLETED)

**Error Responses:**

| Status Code | Mô tả | Response Body |
|-------------|-------|---------------|
| 400 | Goal đã được xác nhận hoàn thành | `{"timestamp": "...", "status": 400, "error": "Bad Request", "message": "Không thể xóa mục tiêu đã hoàn thành"}` |
| 401 | Unauthorized | `{"timestamp": "...", "status": 401, "error": "Unauthorized", "message": "Full authentication is required..."}` |
| 403 | Forbidden (user không sở hữu goal này) | `{"timestamp": "...", "status": 403, "error": "Forbidden", "message": "Forbidden"}` |
| 404 | Goal không tồn tại | `{"timestamp": "...", "status": 404, "error": "Not Found", "message": "Goal not found"}` |
| 500 | Lỗi server nội bộ | `{"timestamp": "...", "status": 500, "error": "Internal Server Error", "message": "..."}` |

**Example Request (qua Gateway):**
```bash
curl -X DELETE http://localhost:8080/finance/v1/goals/a12b34c5-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Ví dụ:**
- Goal có `savedAmount = 5,000,000` và có 3 transaction liên quan (2 INCOME nạp vào, 1 WITHDRAWAL rút ra)
- Khi xóa goal:
  - Xóa tất cả 3 transaction liên quan
  - Xóa goal
  - Số dư tự động đúng vì transaction đã bị xóa (không còn tính vào số dư)

---

### 5. Summary (Tổng hợp Tài chính)

#### 5.1. Lấy tổng hợp tài chính tháng hiện tại

**Endpoint:** `GET /finance/summary/month` (qua Gateway)  
**Service Endpoint:** `GET /api/summary/month` (internal)

#### 5.2. Test JWT Token (Development Only)

**Endpoint:** `GET /finance/summary/test-jwt` (qua Gateway)  
**Service Endpoint:** `GET /api/summary/test-jwt` (internal)

**Mô tả:** Endpoint này dùng để test và debug JWT token trong quá trình development. Trả về thông tin decoded từ JWT token.

**Authentication:** Required (JWT)

**Response 200 OK:**
```json
{
  "sub": "user-uuid",
  "scope": "read write",
  "iss": "auth-service",
  "authenticated": true,
  "message": "JWT token is valid and decoded successfully"
}
```

**Response khi không có token:**
```json
{
  "error": "No JWT token found",
  "authenticated": false
}
```

**Lưu ý:** Endpoint này chỉ nên được sử dụng trong môi trường development/testing. Trong production, nên disable hoặc remove endpoint này.

**Mô tả:** Lấy tổng hợp tài chính của tháng hiện tại bao gồm:
- Số dư hiện tại (tổng thu - tổng chi)
- Thu nhập tháng này
- Chi tiêu tháng này
- Tỷ lệ tiết kiệm (%)

**Authentication:** Required (JWT)

**Response 200 OK:**
```json
{
  "currentBalance": 10000000,
  "monthlyIncome": 15000000,
  "monthlyExpense": 5000000,
  "savingRate": 66.67
}
```

**Business Logic:**
- `currentBalance`: `initialBalance + totalIncome - totalExpense - totalGoalDeposit + totalWithdrawal` (tất cả thời gian)
  - `initialBalance`: Số dư ban đầu (0 nếu chưa khai báo)
  - `totalIncome`: Tổng INCOME transactions **không có goalId** (status = ACTIVE) - cộng vào số dư
  - `totalGoalDeposit`: Tổng INCOME transactions **có goalId** (status = ACTIVE) - trừ khỏi số dư (tiền bị khóa)
  - `totalExpense`: Tổng EXPENSE transactions (status = ACTIVE) - trừ khỏi số dư
  - `totalWithdrawal`: Tổng WITHDRAWAL transactions (status = ACTIVE) - cộng vào số dư (tiền được giải phóng)
- `monthlyIncome`: Tổng INCOME **không có goalId** trong tháng hiện tại
- `monthlyExpense`: Tổng EXPENSE trong tháng hiện tại
- `savingRate`: `((monthlyIncome - monthlyExpense) / monthlyIncome) * 100` (nếu monthlyIncome > 0)

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

## Data Models

### UserBalance Entity

**Table:** `user_balance`

```json
{
  "userId": "UUID (Primary Key)",
  "initialBalance": "BigDecimal",
  "createdAt": "LocalDateTime",
  "updatedAt": "LocalDateTime"
}
```

**Field Descriptions:**
- `userId`: Primary key, UUID, foreign key đến user
- `initialBalance`: Số dư ban đầu, NOT NULL, DECIMAL(19,2), mặc định 0
- `createdAt`: Thời gian tạo, TIMESTAMP, NOT NULL
- `updatedAt`: Thời gian cập nhật, TIMESTAMP, NOT NULL

**Business Logic:**
- Mỗi user chỉ có thể có một record trong bảng này
- Chỉ có thể khai báo số dư ban đầu một lần duy nhất
- Số dư hiện tại = `initialBalance + totalIncome - totalExpense - totalWithdrawal`

---

### Transaction Entity

**Table:** `transactions`

```json
{
  "transactionId": "UUID",
  "userId": "UUID",
  "type": "INCOME | EXPENSE",
  "amount": "BigDecimal",
  "name": "String (max 255)",
  "category": "Category (ManyToOne)",
  "note": "String (TEXT)",
  "transactionDate": "LocalDateTime",
  "goal": "Goal (ManyToOne, nullable)",
  "status": "ACTIVE | DELETED",
  "createdAt": "LocalDateTime",
  "updatedAt": "LocalDateTime"
}
```

**Field Descriptions:**
- `transactionId`: Primary key, UUID
- `userId`: Foreign key đến user, NOT NULL
- `type`: Enum (INCOME, EXPENSE, hoặc WITHDRAWAL), NOT NULL
- `amount`: Số tiền, NOT NULL, DECIMAL trong database
- `name`: Tên giao dịch, NOT NULL, VARCHAR(255)
- `category`: Danh mục, ManyToOne với Category, NOT NULL
- `note`: Ghi chú, TEXT, có thể null
- `transactionDate`: Ngày giao dịch, NOT NULL, TIMESTAMP
- `goal`: Mục tiêu liên kết, ManyToOne với Goal, có thể null (cho INCOME và WITHDRAWAL)
- `status`: Trạng thái, VARCHAR(10), NOT NULL, mặc định "ACTIVE"
- `createdAt`: Thời gian tạo, TIMESTAMP, NOT NULL
- `updatedAt`: Thời gian cập nhật, TIMESTAMP, NOT NULL

---

### Goal Entity

**Table:** `goal`

```json
{
  "goalId": "UUID",
  "userId": "UUID",
  "title": "String (max 255)",
  "amount": "BigDecimal",
  "startAt": "LocalDateTime",
  "endAt": "LocalDateTime",
  "status": "ACTIVE | COMPLETED | FAILED",
  "updatedAt": "LocalDateTime",
  "newStatus": "ACTIVE | COMPLETED | FAILED",
  "savedAmount": "BigDecimal"
}
```

**Field Descriptions:**
- `goalId`: Primary key, UUID
- `userId`: Foreign key đến user, NOT NULL
- `title`: Tên mục tiêu, NOT NULL, VARCHAR(255)
- `amount`: Số tiền mục tiêu, NOT NULL, DECIMAL
- `startAt`: Ngày bắt đầu, TIMESTAMP, NOT NULL
- `endAt`: Hạn hoàn thành, TIMESTAMP, NOT NULL
- `status`: Trạng thái hiện tại, VARCHAR(10), NOT NULL, ENUM('ACTIVE', 'COMPLETED', 'FAILED')
- `updatedAt`: Thời gian cập nhật, TIMESTAMP, NOT NULL
- `newStatus`: Trạng thái mới (internal), VARCHAR(10), NOT NULL
- `savedAmount`: Số tiền đã tiết kiệm, DECIMAL, NOT NULL, mặc định 0

**Business Logic:**
- `savedAmount` được tự động cập nhật khi có INCOME transaction được gắn vào goal
- Status được tự động check và update:
  - COMPLETED: `savedAmount >= amount`
  - FAILED: `endAt < now` và `savedAmount < amount`
  - ACTIVE: Còn lại

---

### Category Entity

**Table:** `category`

```json
{
  "categoryId": "UUID",
  "userId": "UUID",
  "name": "String (max 100)",
  "isDefault": "Boolean",
  "createdAt": "LocalDateTime"
}
```

**Field Descriptions:**
- `categoryId`: Primary key, UUID
- `userId`: Foreign key đến user, NOT NULL
- `name`: Tên danh mục, NOT NULL, VARCHAR(100)
- `isDefault`: Có phải danh mục mặc định không, BOOLEAN, NOT NULL, mặc định false
- `createdAt`: Thời gian tạo, TIMESTAMP, NOT NULL

**Constraints:**
- Unique constraint: `(user_id, name)` - Mỗi user không thể có 2 danh mục cùng tên

---

### TransactionRequestDto

**Request DTO cho Transaction endpoints**

```json
{
  "type": "String (INCOME | EXPENSE) - Required",
  "amount": "BigDecimal - Required",
  "name": "String - Required",
  "categoryId": "UUID - Conditional (Bắt buộc khi không có goalId, Tùy chọn khi có goalId)",
  "note": "String - Optional",
  "goalId": "UUID - Optional (chỉ cho INCOME)",
  "transactionDate": "LocalDateTime - Optional (mặc định now())"
}
```

**Validation Annotations:**
- `type`: `@NotNull`
- `amount`: `@NotNull`
- `name`: `@NotNull`
- `categoryId`: Optional (không có `@NotNull`)
  - **Bắt buộc** khi không có `goalId` (transaction thông thường)
  - **Tùy chọn** khi có `goalId` (nạp vào goal) - nếu không có, hệ thống tự động tạo/gán category "Tiết kiệm"

---

### TransactionResponseDto

**Response DTO cho Transaction endpoints**

```json
{
  "transactionId": "UUID",
  "type": "INCOME | EXPENSE",
  "name": "String",
  "category": "String",
  "note": "String",
  "amount": "BigDecimal",
  "transactionDate": "LocalDateTime",
  "goalId": "UUID (nullable)"
}
```

---

### GoalRequestDto

**Request DTO cho Goal endpoints**

```json
{
  "title": "String - Required",
  "amount": "BigDecimal - Required",
  "endAt": "LocalDateTime - Required",
  "startAt": "LocalDateTime - Optional"
}
```

**Validation Annotations:**
- `title`: `@NotNull`
- `amount`: `@NotNull`
- `endAt`: `@NotNull`
- `startAt`: Optional

---

### GoalStatusUpDate

**Request DTO cho cập nhật trạng thái Goal**

```json
{
  "status": "String (ACTIVE | COMPLETED | FAILED) - Required"
}
```

**Validation Annotations:**
- `status`: `@NotNull`

---

### CategoryRequestDto

**Request DTO cho Category endpoints**

```json
{
  "name": "String - Required",
  "type": "String - Optional (INCOME | EXPENSE | BOTH, default: EXPENSE)"
}
```

**Validation Annotations:**
- `name`: `@NotBlank`
- `type`: Optional, phải là một trong: `INCOME`, `EXPENSE`, `BOTH` (mặc định: `EXPENSE`)

---

### Category

**Entity model cho Category**

```json
{
  "categoryId": "UUID",
  "userId": "UUID",
  "name": "String",
  "type": "CategoryType (INCOME | EXPENSE | BOTH)",
  "isDefault": "Boolean",
  "createdAt": "LocalDateTime"
}
```

**Lưu ý:**
- Category "Khác" là default category (type = BOTH), luôn tồn tại và không thể xóa
- Category "Khác" có thể dùng cho cả INCOME và EXPENSE transactions

---

### BalanceInitializeRequestDto

**Request DTO cho Balance initialize endpoint**

```json
{
  "amount": "BigDecimal - Required (phải > 0)"
}
```

**Validation Annotations:**
- `amount`: `@NotNull`, `@Positive`

---

### BalanceResponseDto

**Response DTO cho Balance endpoint**

```json
{
  "currentBalance": "BigDecimal",
  "initialBalance": "BigDecimal",
  "totalIncome": "BigDecimal",
  "totalGoalDeposit": "BigDecimal",
  "totalExpense": "BigDecimal",
  "totalWithdrawal": "BigDecimal"
}
```

**Field Descriptions:**
- `currentBalance`: Số dư hiện tại = initialBalance + totalIncome - totalExpense - totalGoalDeposit + totalWithdrawal
- `initialBalance`: Số dư ban đầu (0 nếu chưa khai báo)
- `totalIncome`: Tổng thu nhập thông thường (INCOME không có goalId, tất cả thời gian, chỉ ACTIVE) - cộng vào số dư
- `totalGoalDeposit`: Tổng nạp vào goal (INCOME có goalId, tất cả thời gian, chỉ ACTIVE) - trừ khỏi số dư (tiền bị khóa)
- `totalExpense`: Tổng chi tiêu (tất cả thời gian, chỉ ACTIVE) - trừ khỏi số dư
- `totalWithdrawal`: Tổng rút tiền từ goal (tất cả thời gian, chỉ ACTIVE) - cộng vào số dư (tiền được giải phóng)

---

### GoalWithdrawRequestDto

**Request DTO cho Goal withdraw endpoint**

```json
{
  "amount": "BigDecimal - Required (phải > 0)",
  "note": "String - Optional"
}
```

**Validation Annotations:**
- `amount`: `@NotNull`, `@Positive`
- `note`: Optional

---

### SummaryResponseDto

**Response DTO cho Summary endpoint**

```json
{
  "currentBalance": "BigDecimal",
  "monthlyIncome": "BigDecimal",
  "monthlyExpense": "BigDecimal",
  "savingRate": "double"
}
```

---

## Enums

### TransactionType

**Package:** `vn.uth.financeservice.entity.TransactionType`

| Value | Mô tả |
|-------|-------|
| `INCOME` | Thu nhập |
| `EXPENSE` | Chi tiêu |
| `WITHDRAWAL` | Rút tiền từ mục tiêu |

**Usage:**
- Sử dụng trong Transaction entity
- Phải match chính xác (case-sensitive) khi gửi request
- `WITHDRAWAL` transactions được tạo tự động khi user rút tiền từ goal

---

### GoalStatus

**Package:** `vn.uth.financeservice.entity.GoalStatus`

| Value | Mô tả |
|-------|-------|
| `ACTIVE` | Đang thực hiện |
| `COMPLETED` | Đã hoàn thành |
| `FAILED` | Thất bại |

**Usage:**
- Sử dụng trong Goal entity
- Phải match chính xác (case-sensitive) khi gửi request

---

### CategoryType

**Package:** `vn.uth.financeservice.entity.CategoryType`

| Value | Mô tả |
|-------|-------|
| `INCOME` | Chỉ dùng cho khoản thu (INCOME transactions) |
| `EXPENSE` | Chỉ dùng cho khoản chi (EXPENSE transactions) |
| `BOTH` | Dùng cho cả khoản thu và khoản chi (INCOME và EXPENSE transactions) |

**Usage:**
- Sử dụng trong Category entity
- Phải match chính xác (case-sensitive) khi gửi request
- Category "Khác" (default category) luôn có type = BOTH
- Khi tạo transaction, category type phải phù hợp với transaction type:
  - INCOME transaction → category type phải là INCOME hoặc BOTH
  - EXPENSE transaction → category type phải là EXPENSE hoặc BOTH
  - WITHDRAWAL transaction → category type phải là EXPENSE hoặc BOTH (thường dùng category "Rút tiền")

---

## Error Handling

### Error Response Format

Tất cả các lỗi sẽ trả về với format chuẩn của Spring Boot:

```json
{
  "timestamp": "2025-01-19T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: type must be INCOME or EXPENSE",
  "path": "/api/v1/transactions"
}
```

### Common HTTP Status Codes

| Status Code | Mô tả | Khi nào xảy ra |
|-------------|-------|----------------|
| `200 OK` | Thành công | Request thành công |
| `400 Bad Request` | Dữ liệu không hợp lệ | Validation failed, missing required fields |
| `401 Unauthorized` | Chưa xác thực | JWT token không hợp lệ hoặc thiếu |
| `403 Forbidden` | Không có quyền | User không có quyền truy cập resource |
| `404 Not Found` | Không tìm thấy resource | ID không tồn tại trong database |
| `500 Internal Server Error` | Lỗi server | Lỗi không mong đợi từ server |

### Error Examples

**400 Bad Request - Validation Error:**
```json
{
  "timestamp": "2025-01-19T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: type must be INCOME or EXPENSE",
  "path": "/api/v1/transactions"
}
```

**401 Unauthorized:**
```json
{
  "timestamp": "2025-01-19T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/transactions"
}
```

**403 Forbidden:**
```json
{
  "timestamp": "2025-01-19T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Forbidden",
  "path": "/api/v1/transactions/e1f1d8a3-0000-0000-0000-000000000000"
}
```

**404 Not Found:**
```json
{
  "timestamp": "2025-01-19T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Transaction not found",
  "path": "/api/v1/transactions/e1f1d8a3-0000-0000-0000-000000000000"
}
```

---

## Examples

### Example 1: Khai báo số dư ban đầu

**Request:**
```bash
curl -X POST http://localhost:8080/finance/v1/balance/initialize \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 10000000
  }'
```

**Response:**
```json
{
  "userId": "user-uuid",
  "initialBalance": 10000000,
  "createdAt": "2025-01-19T10:30:00",
  "updatedAt": "2025-01-19T10:30:00"
}
```

---

### Example 2: Xem số dư hiện tại

**Request (qua Gateway):**
```bash
curl -X GET http://localhost:8080/finance/v1/balance \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response:**
```json
{
  "currentBalance": 11000000,
  "initialBalance": 10000000,
  "totalIncome": 30000000,
  "totalGoalDeposit": 5000000,
  "totalExpense": 12000000,
  "totalWithdrawal": 2000000
}
```

**Giải thích:**
- `totalIncome`: 30,000,000 (thu nhập thông thường, không có goalId)
- `totalGoalDeposit`: 5,000,000 (nạp vào goal, bị khóa)
- `totalExpense`: 12,000,000 (chi tiêu)
- `totalWithdrawal`: 2,000,000 (rút từ goal, được giải phóng)
- `currentBalance`: 10,000,000 + 30,000,000 - 12,000,000 - 5,000,000 + 2,000,000 = 11,000,000

---

### Example 3: Tạo giao dịch thu nhập

**Request (qua Gateway):**
```bash
curl -X POST http://localhost:8080/finance/v1/transactions \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "type": "INCOME",
    "amount": 5000000,
    "name": "Lương tháng 1",
    "categoryId": "c1d2e3f4-0000-0000-0000-000000000000",
    "note": "Lương cơ bản"
  }'
```

**Response:**
```json
{
  "transactionId": "e1f1d8a3-0000-0000-0000-000000000000",
  "type": "INCOME",
  "name": "Lương tháng 1",
  "category": "Salary",
  "note": "Lương cơ bản",
  "amount": 5000000,
  "transactionDate": "2025-01-19T10:30:00",
  "goalId": null
}
```

---

### Example 4: Tạo giao dịch chi tiêu

**Request (qua Gateway):**
```bash
curl -X POST http://localhost:8080/finance/v1/transactions \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "type": "EXPENSE",
    "amount": 500000,
    "name": "Mua sắm",
    "categoryId": "d2e3f4g5-0000-0000-0000-000000000001",
    "note": "Mua quần áo"
  }'
```

---

### Example 5: Nạp tiền vào goal (không cần categoryId)

**Request (qua Gateway):**
```bash
curl -X POST http://localhost:8080/finance/v1/transactions \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "type": "INCOME",
    "amount": 2000000,
    "name": "Nạp tiền",
    "goalId": "a12b34c5-0000-0000-0000-000000000000"
  }'
```

**Note:** 
- `categoryId` không cần thiết khi nạp vào goal - hệ thống tự động tạo/gán category "Tiết kiệm"
- `savedAmount` của goal sẽ được tự động cập nhật và `newStatus` sẽ được check
- Số tiền nạp vào goal sẽ **bị trừ khỏi số dư hiện tại** (tiền bị khóa trong goal)
- **Validation:** Kiểm tra số dư hiện tại >= số tiền thực tế sẽ nạp
- **Logic nạp dư:** Nếu nạp > số tiền còn lại để hoàn thành → chỉ nạp đủ số tiền còn lại, số dư thừa không bị trừ
- **Không cho phép:** Nạp nếu goal đã COMPLETED (đã xác nhận) hoặc goal đã đủ tiền (`savedAmount >= amount`)

**Request với categoryId tùy chọn (nếu muốn chọn category khác):**
```bash
curl -X POST http://localhost:8080/finance/v1/transactions \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "type": "INCOME",
    "amount": 2000000,
    "name": "Tiết kiệm tháng 1",
    "categoryId": "c1d2e3f4-0000-0000-0000-000000000000",
    "note": "Tiết kiệm cho goal",
    "goalId": "a12b34c5-0000-0000-0000-000000000000"
  }'
```

---

### Example 6: Lấy danh sách giao dịch gần đây

**Request (qua Gateway):**
```bash
curl -X GET "http://localhost:8080/finance/v1/transactions/recent?limit=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### Example 7: Lấy danh sách giao dịch với phân trang

**Request (qua Gateway):**
```bash
curl -X GET "http://localhost:8080/finance/v1/transactions?page=0&size=20&startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### Example 8: Tạo danh mục mới

**Request (qua Gateway):**
```bash
curl -X POST http://localhost:8080/finance/v1/categories \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Entertainment"
  }'
```

---

### Example 9: Tạo mục tiêu mới

**Request (qua Gateway):**
```bash
curl -X POST http://localhost:8080/finance/v1/goals \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Mua laptop mới",
    "amount": 15000000,
    "endAt": "2025-12-31T00:00:00"
  }'
```

---

### Example 10: Rút tiền từ mục tiêu

**Request (qua Gateway):**
```bash
curl -X POST http://localhost:8080/finance/v1/goals/a12b34c5-0000-0000-0000-000000000000/withdraw \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000000,
    "note": "Cần gấp cho việc khẩn cấp"
  }'
```

**Response:**
```json
{
  "transactionId": "w1x2y3z4-0000-0000-0000-000000000000",
  "type": "WITHDRAWAL",
  "name": "Rút từ mục tiêu: Mua laptop mới",
  "category": "Rút tiền",
  "note": "Cần gấp cho việc khẩn cấp",
  "amount": 5000000,
  "transactionDate": "2025-01-19T10:30:00",
  "goalId": "a12b34c5-0000-0000-0000-000000000000"
}
```

**Note:** 
- `savedAmount` của goal sẽ giảm từ 15,000,000 → 10,000,000
- Số dư hiện tại sẽ **tăng thêm 5,000,000** (tiền được giải phóng từ goal)
- Goal `newStatus` sẽ được tự động check và update (set = COMPLETED nếu đạt mục tiêu, nhưng status vẫn ACTIVE)
- **Không cho phép:** Rút nếu goal đã COMPLETED (đã xác nhận)

---

### Example 12: Xác nhận hoàn thành mục tiêu

**Request (qua Gateway):**
```bash
curl -X POST http://localhost:8080/finance/v1/goals/a12b34c5-0000-0000-0000-000000000000/confirm-completion \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response:**
```json
{
  "goalId": "a12b34c5-0000-0000-0000-000000000000",
  "userId": "user-uuid",
  "title": "Mua laptop mới",
  "amount": 15000000,
  "startAt": "2025-01-01T00:00:00",
  "endAt": "2025-12-31T00:00:00",
  "status": "COMPLETED",
  "updatedAt": "2025-01-19T10:30:00",
  "newStatus": "COMPLETED",
  "savedAmount": 15000000
}
```

**Note:**
- Chỉ cho phép xác nhận khi `savedAmount >= amount`
- Sau khi xác nhận, goal chuyển sang COMPLETED và không thể thao tác (xóa, rút, nạp) nữa
- Nếu chưa xác nhận, goal vẫn ở trạng thái ACTIVE (có thể xóa, rút, nạp)

---

### Example 13: Xóa mục tiêu

**Request (qua Gateway):**
```bash
curl -X DELETE http://localhost:8080/finance/v1/goals/a12b34c5-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response:**
```json
(Empty body - 200 OK)
```

**Note:**
- Xóa tất cả transaction liên quan đến goal
- Xóa goal
- Số dư tự động đúng vì transaction đã bị xóa (không còn tính vào số dư)
- **Không cho phép:** Xóa nếu goal đã COMPLETED (đã xác nhận)

---

### Example 11: Lấy tổng hợp tài chính

**Request (qua Gateway):**
```bash
curl -X GET http://localhost:8080/finance/summary/month \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response:**
```json
{
  "currentBalance": 10000000,
  "monthlyIncome": 15000000,
  "monthlyExpense": 5000000,
  "savingRate": 66.67
}
```

**Note:** `monthlyIncome` chỉ tính thu nhập thông thường (INCOME không có goalId). Thu nhập nạp vào goal không được tính vào `monthlyIncome`.

---

## Configuration

### Application Properties

**File:** `src/main/resources/application.properties`

```properties
# Service Configuration
spring.application.name=finance-service
server.port=8202

# Eureka Configuration
eureka.client.service-url.default-zone=http://localhost:8761/eureka

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/testdb
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update

# JWT Configuration
app.jwt.secret=dummy-finance-service-secret-key-1234567890-change-in-production

# CORS Configuration
app.cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

### Important Notes

1. **JWT Secret**: Phải thay đổi `app.jwt.secret` trong production bằng một secret key mạnh (ít nhất 32 ký tự). Secret này phải giống với secret trong auth-service.

2. **CORS Origins**: Có thể thêm nhiều origins bằng cách phân tách bằng dấu phẩy:
   ```properties
   app.cors.allowed-origins=http://localhost:3000,http://localhost:5173,https://yourdomain.com
   ```

3. **Database**: Đảm bảo MySQL database đang chạy và có database `testdb` (hoặc thay đổi trong config).

---

## Notes

### 1. Authentication

- Tất cả endpoints (trừ public endpoints) yêu cầu JWT token hợp lệ
- JWT token phải được tạo bởi auth-service với cùng secret key
- Token phải có `subject` (sub) claim chứa UUID của user

### 2. Goal Status và Xác Nhận Hoàn Thành

- Goal status được tự động check và update khi:
  - Gọi `GET /finance/v1/goals` (qua Gateway) hoặc `GET /api/v1/goals` (service)
  - Có INCOME transaction được gắn vào goal
  - Có transaction được xóa khỏi goal
  - Có WITHDRAWAL transaction được tạo từ goal
- **Goal Completion Logic:**
  - Khi `savedAmount >= amount`, `newStatus` sẽ được set = COMPLETED (nhưng `status` vẫn ACTIVE)
  - User phải xác nhận hoàn thành thông qua API `POST /{id}/confirm-completion` để chuyển `status` sang COMPLETED
  - Nếu chưa xác nhận, goal vẫn ở trạng thái ACTIVE (có thể xóa, rút, nạp)
  - Sau khi xác nhận COMPLETED, goal không thể thao tác (xóa, rút, nạp) nữa
- **Goal Status:**
  - `ACTIVE`: Đang thực hiện (có thể thao tác)
  - `COMPLETED`: Đã hoàn thành (đã xác nhận, không thể thao tác)
  - `FAILED`: Thất bại (hết hạn mà chưa đạt mục tiêu)

### 3. Transaction-Goal Relationship

- Chỉ INCOME transactions mới có thể được gắn vào goal
- Khi INCOME transaction được gắn vào goal, `savedAmount` của goal sẽ tự động tăng
- Khi transaction được xóa, `savedAmount` sẽ tự động giảm
- **Nạp tiền vào goal:**
  - Không cho phép nạp nếu goal đã COMPLETED (đã xác nhận)
  - Không cho phép nạp nếu goal đã đủ tiền (`savedAmount >= amount`)
  - Nếu nạp > số tiền còn lại để hoàn thành → chỉ nạp đủ số tiền còn lại
  - Số dư thừa không bị trừ (chỉ tạo 1 transaction với số tiền vừa đủ)

### 4. Xóa Goal

- Khi xóa goal, hệ thống sẽ:
  - Tìm tất cả transaction liên quan đến goal
  - Xóa tất cả transaction đó
  - Xóa goal
- Số dư tự động đúng vì transaction đã bị xóa (không còn tính vào số dư)
- Không cho phép xóa goal đã được xác nhận hoàn thành (COMPLETED)

### 5. Date/Time Format

Sử dụng ISO 8601 format cho LocalDateTime:
- Format: `yyyy-MM-ddTHH:mm:ss`
- Example: `2025-12-31T00:00:00`
- Timezone: Sử dụng server timezone (mặc định)

### 6. UUID Format

Tất cả UUID phải theo format chuẩn:
- Format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
- Example: `e1f1d8a3-0000-0000-0000-000000000000`
- Case: Không phân biệt hoa thường

### 7. Category Type và Validation

**Category Type:**
- `INCOME`: Chỉ dùng cho khoản thu (INCOME transactions)
- `EXPENSE`: Chỉ dùng cho khoản chi (EXPENSE transactions)
- `BOTH`: Dùng cho cả khoản thu và khoản chi (INCOME và EXPENSE transactions)

**Category "Khác":**
- Category "Khác" là default category (type = BOTH), luôn tồn tại và không thể xóa
- Category "Khác" có thể dùng cho cả INCOME và EXPENSE transactions
- Khi xóa category, tất cả transaction đang sử dụng category đó sẽ tự động chuyển sang category "Khác"

**Validation khi tạo Transaction:**
- INCOME transaction → category type phải là `INCOME` hoặc `BOTH`
- EXPENSE transaction → category type phải là `EXPENSE` hoặc `BOTH`
- WITHDRAWAL transaction → category type phải là `EXPENSE` hoặc `BOTH` (thường dùng category "Rút tiền")
- Nếu category type không phù hợp, hệ thống sẽ trả về lỗi validation

**Xóa Category:**
- Khi xóa category, tất cả transaction (cả ACTIVE và DELETED) đang sử dụng category đó sẽ tự động chuyển sang category "Khác"
- Lịch sử transaction được giữ nguyên, chỉ category được thay đổi
- Không cho phép xóa category "Khác" (default category)
- Không cho phép xóa default categories khác

### 8. Validation Số Dư

**Số dư luôn >= 0:**
- Hệ thống không cho phép số dư âm
- Khi tạo EXPENSE transaction: Kiểm tra số dư hiện tại >= `amount` (nếu không đủ → 400)
- Khi nạp tiền vào goal (INCOME transaction có goalId): Kiểm tra số dư hiện tại >= số tiền thực tế sẽ nạp (nếu không đủ → 400)
- INCOME transaction thông thường (không có goalId): Không cần validate (luôn cộng vào số dư)

### 9. Pagination

- Page number bắt đầu từ 0
- Default page size: 15
- Response format theo Spring Data Page

---

## Version History

### v1.3.0 (2025-11-23)

**Updates:**
- ✅ Xóa API PUT /{id}/status (update status manually)
- ✅ Thêm API POST /{id}/confirm-completion để xác nhận hoàn thành mục tiêu
- ✅ Goal chỉ chuyển sang COMPLETED khi user xác nhận (không tự động)
- ✅ Validation số dư: không cho phép số dư âm
- ✅ Logic nạp tiền vào goal: validate goal chưa đủ, nạp dư chỉ nạp đủ
- ✅ Không cho phép xóa, rút, nạp nếu goal đã COMPLETED (đã xác nhận)

### v1.2.0 (2025-11-23)

**Updates:**
- ✅ Thêm Category Type (INCOME, EXPENSE, BOTH) để phân loại category
- ✅ Category "Khác" là default category (type = BOTH), không thể xóa
- ✅ Logic xóa category: tự động chuyển transaction sang category "Khác"
- ✅ Validation: category type phải phù hợp với transaction type
- ✅ Cập nhật CategoryRequestDto: thêm field `type` (optional, default: EXPENSE)

### v1.1.0 (2025-11-23)

**Updates:**
- ✅ Thêm Balance Management (Initial Balance, Current Balance)
- ✅ Thêm Withdrawal từ Goal feature
- ✅ Thêm Delete Goal feature (xóa tất cả transaction liên quan)
- ✅ Cập nhật Summary calculation với initialBalance và totalWithdrawal
- ✅ Thêm GlobalExceptionHandler với proper HTTP status codes
- ✅ Cập nhật API documentation với Gateway routing information
- ✅ Thêm test-jwt endpoint cho development
- ✅ CategoryId optional khi nạp vào goal (tự động tạo category "Tiết kiệm")
- ✅ Cập nhật logic tính số dư: nạp vào goal trừ khỏi số dư, rút từ goal cộng vào số dư
- ✅ Thêm totalGoalDeposit vào BalanceResponseDto

### v1.0.0 (2025-01-19)

**Initial Release:**
- ✅ Transaction management endpoints
- ✅ Category management endpoints
- ✅ Goal management endpoints với auto status update
- ✅ Summary endpoint
- ✅ JWT authentication
- ✅ CORS configuration

---

## Contact & Support

**Development Team:** EduFinAI Development Team

**Service Repository:** finance-service

**For issues and questions:**
- Check service logs
- Review this documentation
- Contact development team

---

**Document Generated:** 2025-01-19  
**Last Updated:** 2025-11-23  
**API Version:** 1.3.0

---

## 📝 Changelog

### v1.3.0 (2025-11-23)

**Goal Completion và Validation Số Dư:**
1. **Xác nhận hoàn thành mục tiêu:**
   - **Mới:** Thêm API `POST /{id}/confirm-completion` để xác nhận hoàn thành mục tiêu
   - Goal chỉ chuyển sang COMPLETED khi user xác nhận (không tự động)
   - Nếu chưa xác nhận, goal vẫn ở trạng thái ACTIVE (có thể xóa, rút, nạp)
   - Sau khi xác nhận COMPLETED, goal không thể thao tác (xóa, rút, nạp) nữa

2. **Xóa API update status:**
   - **Đã xóa:** API `PUT /{id}/status` (update status manually)
   - Status chỉ được thay đổi thông qua xác nhận hoàn thành hoặc tự động (FAILED)

3. **Validation số dư:**
   - **Mới:** Hệ thống không cho phép số dư âm
   - Khi tạo EXPENSE transaction: Kiểm tra số dư hiện tại >= `amount`
   - Khi nạp tiền vào goal: Kiểm tra số dư hiện tại >= số tiền thực tế sẽ nạp

4. **Logic nạp tiền vào goal:**
   - **Mới:** Không cho phép nạp nếu goal đã COMPLETED (đã xác nhận)
   - **Mới:** Không cho phép nạp nếu goal đã đủ tiền (`savedAmount >= amount`)
   - **Mới:** Nếu nạp > số tiền còn lại để hoàn thành → chỉ nạp đủ số tiền còn lại
   - Số dư thừa không bị trừ (chỉ tạo 1 transaction với số tiền vừa đủ)

5. **Logic xóa và rút goal:**
   - **Mới:** Không cho phép xóa nếu goal đã COMPLETED (đã xác nhận)
   - **Mới:** Không cho phép rút nếu goal đã COMPLETED (đã xác nhận)

6. **Goal Status Logic:**
   - Khi `savedAmount >= amount`, `newStatus` sẽ được set = COMPLETED (nhưng `status` vẫn ACTIVE)
   - User phải xác nhận hoàn thành để chuyển `status` sang COMPLETED
   - `newStatus` dùng để frontend biết có thể xác nhận hoàn thành

### v1.2.0 (2025-11-23)

**Category Type Feature:**
1. **Category Type:**
   - **Mới:** Thêm field `type` vào Category entity (INCOME, EXPENSE, BOTH)
   - Category "Khác" là default category (type = BOTH), luôn tồn tại và không thể xóa
   - Category "Khác" có thể dùng cho cả INCOME và EXPENSE transactions

2. **Xóa Category:**
   - **Trước:** Xóa category sẽ bị lỗi foreign key constraint nếu có transaction sử dụng
   - **Sau:** Khi xóa category, tất cả transaction (cả ACTIVE và DELETED) tự động chuyển sang category "Khác"
   - Lịch sử transaction được giữ nguyên, chỉ category được thay đổi

3. **Validation:**
   - **Mới:** Khi tạo transaction, category type phải phù hợp với transaction type:
     - INCOME transaction → category type phải là INCOME hoặc BOTH
     - EXPENSE transaction → category type phải là EXPENSE hoặc BOTH
     - WITHDRAWAL transaction → category type phải là EXPENSE hoặc BOTH

4. **CategoryRequestDto:**
   - **Mới:** Thêm field `type` (optional, default: EXPENSE)
   - Nếu không có `type`, mặc định là EXPENSE

### v1.1.0 (2025-11-23)

### Thay đổi quan trọng:

1. **CategoryId khi nạp vào goal:**
   - **Trước:** Bắt buộc phải có `categoryId` khi tạo transaction
   - **Sau:** `categoryId` là tùy chọn khi nạp vào goal (có `goalId`). Nếu không có, hệ thống tự động tạo/gán category "Tiết kiệm"

2. **Logic tính số dư với goal:**
   - **Trước:** Nạp vào goal = cộng vào số dư, rút từ goal = trừ khỏi số dư
   - **Sau:** Nạp vào goal = **trừ khỏi số dư** (tiền bị khóa), rút từ goal = **cộng vào số dư** (tiền được giải phóng)

3. **BalanceResponseDto:**
   - Thêm field `totalGoalDeposit` - tổng nạp vào goal
   - `totalIncome` giờ chỉ tính thu nhập thông thường (không có goalId)

4. **Summary:**
   - `monthlyIncome` chỉ tính thu nhập thông thường (INCOME không có goalId)
   - Logic tính `currentBalance` đã được cập nhật

5. **Xóa Goal:**
   - **Mới:** Thêm endpoint `DELETE /finance/v1/goals/{id}` để xóa goal
   - Khi xóa goal, hệ thống tự động xóa tất cả transaction liên quan đến goal đó
   - Số dư tự động đúng vì transaction đã bị xóa (không còn tính vào số dư)

6. **Category Type (v1.2.0):**
   - **Mới:** Thêm field `type` vào Category (INCOME, EXPENSE, BOTH)
   - Category "Khác" là default category (type = BOTH), luôn tồn tại và không thể xóa
   - Khi xóa category, tất cả transaction tự động chuyển sang category "Khác"
   - Validation: category type phải phù hợp với transaction type khi tạo transaction

