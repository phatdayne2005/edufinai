# Finance Service API Documentation

## 📋 Mục lục

1. [Tổng quan](#tổng-quan)
2. [Authentication](#authentication)
3. [CORS Configuration](#cors-configuration)
4. [Endpoints](#endpoints)
   - [Transaction Management](#1-transaction-management-quản-lý-giao-dịch)
   - [Category Management](#2-category-management-quản-lý-danh-mục)
   - [Goal Management](#3-goal-management-quản-lý-mục-tiêu-tài-chính)
   - [Summary](#4-summary-tổng-hợp-tài-chính)
5. [Data Models](#data-models)
6. [Enums](#enums)
7. [Error Handling](#error-handling)
8. [Examples](#examples)
9. [Configuration](#configuration)

---

## Tổng quan

Finance Service là một microservice trong hệ thống EduFinAI, chịu trách nhiệm quản lý:
- **Giao dịch tài chính** (Thu nhập và Chi tiêu)
- **Danh mục** (Categories)
- **Mục tiêu tài chính** (Financial Goals)
- **Tổng hợp tài chính** (Financial Summary)

**Base URL:** `http://localhost:8202`  
**API Version:** v1  
**Port:** 8202  
**Service Name:** finance-service  
**Eureka Registration:** `http://localhost:8761/eureka`

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

**Example Request:**
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

### 1. Transaction Management (Quản lý Giao dịch)

#### 1.1. Tạo giao dịch mới

**Endpoint:** `POST /api/v1/transactions`

**Mô tả:** Tạo một giao dịch thu nhập hoặc chi tiêu mới.

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "type": "INCOME",                    // Bắt buộc: "INCOME" hoặc "EXPENSE"
  "amount": 5000000,                   // Bắt buộc: Số tiền (BigDecimal)
  "name": "Lương tháng 1",            // Bắt buộc: Tên giao dịch (String)
  "categoryId": "uuid-category-id",   // Bắt buộc: ID danh mục (UUID)
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
- `categoryId`: Bắt buộc, phải là UUID hợp lệ và tồn tại
- `goalId`: Tùy chọn, chỉ áp dụng cho INCOME transactions
- `transactionDate`: Tùy chọn, format ISO 8601 (yyyy-MM-ddTHH:mm:ss)

**Business Logic:**
- Nếu `goalId` được cung cấp và `type` là "INCOME", transaction sẽ được gắn vào goal và `savedAmount` của goal sẽ được cập nhật tự động
- Goal status sẽ được tự động check và update (COMPLETED nếu đạt mục tiêu)

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 400 | Dữ liệu không hợp lệ (validation failed) |
| 401 | Unauthorized (thiếu hoặc JWT token không hợp lệ) |
| 404 | Category không tồn tại |
| 500 | Lỗi server nội bộ |

---

#### 1.2. Xóa giao dịch

**Endpoint:** `DELETE /api/v1/transactions/{id}`

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

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 403 | Forbidden (user không sở hữu transaction này) |
| 404 | Transaction không tồn tại |
| 500 | Lỗi server nội bộ |

---

#### 1.3. Lấy danh sách giao dịch gần đây

**Endpoint:** `GET /api/v1/transactions/recent`

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

#### 1.4. Lấy danh sách giao dịch (có phân trang)

**Endpoint:** `GET /api/v1/transactions`

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

**Example Request:**
```bash
GET /api/v1/transactions?page=0&size=20&startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59
```

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

### 2. Category Management (Quản lý Danh mục)

#### 2.1. Lấy danh sách danh mục

**Endpoint:** `GET /api/v1/categories`

**Mô tả:** Lấy danh sách tất cả các danh mục của user hiện tại.

**Authentication:** Required (JWT)

**Response 200 OK:**
```json
[
  {
    "categoryId": "c1d2e3f4-0000-0000-0000-000000000000",
    "userId": "user-uuid",
    "name": "Salary",
    "isDefault": false,
    "createdAt": "2025-01-01T00:00:00"
  },
  {
    "categoryId": "d2e3f4g5-0000-0000-0000-000000000001",
    "userId": "user-uuid",
    "name": "Shopping",
    "isDefault": false,
    "createdAt": "2025-01-01T00:00:00"
  }
]
```

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

#### 2.2. Tạo danh mục mới

**Endpoint:** `POST /api/v1/categories`

**Mô tả:** Tạo một danh mục mới cho user.

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "name": "Entertainment"  // Bắt buộc: Tên danh mục (String, không được rỗng)
}
```

**Response 200 OK:**
```json
{
  "categoryId": "e3f4g5h6-0000-0000-0000-000000000002",
  "userId": "user-uuid",
  "name": "Entertainment",
  "isDefault": false,
  "createdAt": "2025-01-19T10:30:00"
}
```

**Validation Rules:**
- `name`: Bắt buộc, không được rỗng (NotBlank)
- Tên danh mục phải unique cho mỗi user (unique constraint: user_id + name)

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 400 | Dữ liệu không hợp lệ hoặc danh mục đã tồn tại |
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

#### 2.3. Xóa danh mục

**Endpoint:** `DELETE /api/v1/categories/{id}`

**Mô tả:** Xóa một danh mục. Chỉ user sở hữu danh mục mới có thể xóa.

**Authentication:** Required (JWT)

**Path Parameters:**
- `id` (UUID, required): ID của danh mục cần xóa

**Response 200 OK:**
```json
(Empty body)
```

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 403 | Forbidden (user không sở hữu category này) |
| 404 | Category không tồn tại |
| 500 | Lỗi server nội bộ |

---

### 3. Goal Management (Quản lý Mục tiêu Tài chính)

#### 3.1. Tạo mục tiêu mới

**Endpoint:** `POST /api/v1/goals`

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

#### 3.2. Lấy danh sách mục tiêu

**Endpoint:** `GET /api/v1/goals`

**Mô tả:** Lấy danh sách tất cả các mục tiêu của user. Status sẽ được tự động check và update:
- **COMPLETED**: Nếu `savedAmount >= amount`
- **FAILED**: Nếu `endAt < now` và `savedAmount < amount`
- **ACTIVE**: Còn lại

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
    "newStatus": "ACTIVE",
    "savedAmount": 5000000
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
- Status được tự động check và update mỗi khi gọi endpoint này
- `savedAmount` được cập nhật tự động khi có INCOME transaction được gắn vào goal

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

#### 3.3. Cập nhật trạng thái mục tiêu

**Endpoint:** `PUT /api/v1/goals/{id}/status`

**Mô tả:** Cập nhật trạng thái của một mục tiêu. Chỉ user sở hữu mục tiêu mới có thể cập nhật.

**Authentication:** Required (JWT)

**Path Parameters:**
- `id` (UUID, required): ID của mục tiêu cần cập nhật

**Request Body:**
```json
{
  "status": "COMPLETED"  // Bắt buộc: "ACTIVE", "COMPLETED", hoặc "FAILED" (case-sensitive)
}
```

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
- `status`: Bắt buộc, phải là một trong: "ACTIVE", "COMPLETED", "FAILED" (case-sensitive)
- User chỉ có thể cập nhật mục tiêu của chính mình

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 400 | Status không hợp lệ |
| 401 | Unauthorized |
| 403 | Forbidden (user không sở hữu goal này) |
| 404 | Goal không tồn tại |
| 500 | Lỗi server nội bộ |

---

### 4. Summary (Tổng hợp Tài chính)

#### 4.1. Lấy tổng hợp tài chính tháng hiện tại

**Endpoint:** `GET /api/summary/month`

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
- `currentBalance`: Tổng tất cả INCOME - tổng tất cả EXPENSE (tất cả thời gian)
- `monthlyIncome`: Tổng INCOME trong tháng hiện tại
- `monthlyExpense`: Tổng EXPENSE trong tháng hiện tại
- `savingRate`: `((monthlyIncome - monthlyExpense) / monthlyIncome) * 100` (nếu monthlyIncome > 0)

**Error Responses:**

| Status Code | Mô tả |
|-------------|-------|
| 401 | Unauthorized |
| 500 | Lỗi server nội bộ |

---

## Data Models

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
- `type`: Enum (INCOME hoặc EXPENSE), NOT NULL
- `amount`: Số tiền, NOT NULL, DECIMAL trong database
- `name`: Tên giao dịch, NOT NULL, VARCHAR(255)
- `category`: Danh mục, ManyToOne với Category, NOT NULL
- `note`: Ghi chú, TEXT, có thể null
- `transactionDate`: Ngày giao dịch, NOT NULL, TIMESTAMP
- `goal`: Mục tiêu liên kết, ManyToOne với Goal, có thể null (chỉ cho INCOME)
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
  "categoryId": "UUID - Required",
  "note": "String - Optional",
  "goalId": "UUID - Optional (chỉ cho INCOME)",
  "transactionDate": "LocalDateTime - Optional (mặc định now())"
}
```

**Validation Annotations:**
- `type`: `@NotNull`
- `amount`: `@NotNull`
- `name`: `@NotNull`
- `categoryId`: `@NotNull`

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
  "name": "String - Required"
}
```

**Validation Annotations:**
- `name`: `@NotBlank`

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

**Usage:**
- Sử dụng trong Transaction entity
- Phải match chính xác (case-sensitive) khi gửi request

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

### Example 1: Tạo giao dịch thu nhập

**Request:**
```bash
curl -X POST http://localhost:8202/api/v1/transactions \
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

### Example 2: Tạo giao dịch chi tiêu

**Request:**
```bash
curl -X POST http://localhost:8202/api/v1/transactions \
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

### Example 3: Tạo giao dịch thu nhập gắn vào goal

**Request:**
```bash
curl -X POST http://localhost:8202/api/v1/transactions \
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

**Note:** `savedAmount` của goal sẽ được tự động cập nhật và status sẽ được check.

---

### Example 4: Lấy danh sách giao dịch gần đây

**Request:**
```bash
curl -X GET "http://localhost:8202/api/v1/transactions/recent?limit=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### Example 5: Lấy danh sách giao dịch với phân trang

**Request:**
```bash
curl -X GET "http://localhost:8202/api/v1/transactions?page=0&size=20&startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### Example 6: Tạo danh mục mới

**Request:**
```bash
curl -X POST http://localhost:8202/api/v1/categories \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Entertainment"
  }'
```

---

### Example 7: Tạo mục tiêu mới

**Request:**
```bash
curl -X POST http://localhost:8202/api/v1/goals \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Mua laptop mới",
    "amount": 15000000,
    "endAt": "2025-12-31T00:00:00"
  }'
```

---

### Example 8: Lấy tổng hợp tài chính

**Request:**
```bash
curl -X GET http://localhost:8202/api/summary/month \
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

### 2. Goal Auto Status Update

- Goal status được tự động check và update khi:
  - Gọi `GET /api/v1/goals`
  - Có INCOME transaction được gắn vào goal
  - Có transaction được xóa khỏi goal

### 3. Transaction-G goal Relationship

- Chỉ INCOME transactions mới có thể được gắn vào goal
- Khi INCOME transaction được gắn vào goal, `savedAmount` của goal sẽ tự động tăng
- Khi transaction được xóa, `savedAmount` sẽ tự động giảm

### 4. Date/Time Format

Sử dụng ISO 8601 format cho LocalDateTime:
- Format: `yyyy-MM-ddTHH:mm:ss`
- Example: `2025-12-31T00:00:00`
- Timezone: Sử dụng server timezone (mặc định)

### 5. UUID Format

Tất cả UUID phải theo format chuẩn:
- Format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
- Example: `e1f1d8a3-0000-0000-0000-000000000000`
- Case: Không phân biệt hoa thường

### 6. Pagination

- Page number bắt đầu từ 0
- Default page size: 15
- Response format theo Spring Data Page

---

## Version History

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
**Last Updated:** 2025-01-19  
**API Version:** 1.0.0

