# Gamification Service API Documentation

## Thông tin tổng quan

- **Service Name**: Gamification Service
- **Direct Base URL**: `http://localhost:8203/api/v1/gamify`
- **Gateway Base URL**: `http://localhost:8080/gamification`
- **Version**: 1.0.0
- **Content-Type**: `application/json`

**Lưu ý**: Tất cả requests qua Gateway sẽ được rewrite từ `/gamification/**` → `/api/v1/gamify/**`

---

## Mục lục

1. [Reward APIs](#1-reward-apis)
2. [Leaderboard APIs](#2-leaderboard-apis)
3. [Challenge APIs](#3-challenge-apis)
4. [Authentication & Testing](#4-authentication--testing)

---

## 1. Reward APIs

### 1.1. Thêm phần thưởng cho user

**Endpoint**: `POST /api/v1/gamify/reward`

**Mô tả**: Thêm điểm thưởng và badge cho một user cụ thể.

**Request Body**:

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "badge": "QUIZ_MASTER",
  "score": 100,
  "reason": "Hoàn thành 10 quiz trong ngày"
}
```

**Request Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `userId` | UUID | ✅ Yes | ID của user nhận thưởng |
| `badge` | String | ✅ Yes | Tên badge (không được để trống) |
| `score` | Integer | ✅ Yes | Điểm số được cộng (không được null) |
| `reason` | String | ❌ No | Lý do trao thưởng (tùy chọn) |

**Response** (200 OK):

```json
{
  "rewardId": "660e8400-e29b-41d4-a716-446655440001",
  "status": "SUCCESS"
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `rewardId` | UUID | ID của bản ghi reward vừa tạo |
| `status` | String | Trạng thái xử lý (thường là "SUCCESS") |

**Error Responses**:

- **400 Bad Request**: Dữ liệu request không hợp lệ
  ```json
  {
    "timestamp": "2025-01-15T10:30:00Z",
    "status": 400,
    "error": "Bad Request",
    "message": "User ID không được để trống",
    "path": "/api/v1/gamify/reward"
  }
  ```

- **500 Internal Server Error**: Lỗi server

**Example cURL**:

```bash
# Qua Gateway
curl -X POST http://localhost:8080/gamification/reward \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "badge": "QUIZ_MASTER",
    "score": 100,
    "reason": "Hoàn thành 10 quiz trong ngày"
  }'

# Trực tiếp
curl -X POST http://localhost:8203/api/v1/gamify/reward \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "badge": "QUIZ_MASTER",
    "score": 100,
    "reason": "Hoàn thành 10 quiz trong ngày"
  }'
```

---

### 1.2. Lấy thông tin phần thưởng của user

**Endpoint**: `GET /api/v1/gamify/reward/{userId}`

**Mô tả**: Lấy tổng điểm thưởng và chi tiết các phần thưởng của một user.

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | UUID | ✅ Yes | ID của user cần tra cứu |

**Response** (200 OK):

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "totalScore": 1250.0,
  "rewardDetail": [
    {
      "rewardId": "660e8400-e29b-41d4-a716-446655440001",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "badge": "QUIZ_MASTER",
      "score": 100,
      "reason": "Hoàn thành 10 quiz trong ngày",
      "createdAt": "2025-01-15T10:30:00"
    },
    {
      "rewardId": "770e8400-e29b-41d4-a716-446655440002",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "badge": "STREAK_7",
      "score": 50,
      "reason": "7 ngày liên tiếp hoàn thành quiz",
      "createdAt": "2025-01-14T09:15:00"
    }
  ]
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `userId` | UUID | ID của user |
| `totalScore` | Double | Tổng điểm thưởng hiện tại của user |
| `rewardDetail` | Array | Danh sách chi tiết các phần thưởng đã nhận |
| `rewardDetail[].rewardId` | UUID | ID của phần thưởng |
| `rewardDetail[].userId` | UUID | ID của user nhận thưởng |
| `rewardDetail[].badge` | String | Tên badge |
| `rewardDetail[].score` | Integer | Điểm số của phần thưởng |
| `rewardDetail[].reason` | String | Lý do trao thưởng |
| `rewardDetail[].createdAt` | DateTime | Thời gian nhận thưởng |

**Error Responses**:

- **404 Not Found**: Không tìm thấy user hoặc user chưa có reward nào
- **500 Internal Server Error**: Lỗi server

**Example cURL**:

```bash
# Qua Gateway
curl -X GET http://localhost:8080/gamification/reward/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>"

# Trực tiếp
curl -X GET http://localhost:8203/api/v1/gamify/reward/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>"
```

---

## 2. Leaderboard APIs

### 2.1. Lấy bảng xếp hạng

**Endpoint**: `GET /api/v1/gamify/leaderboard/{type}/{topNumber}`

**Mô tả**: Lấy danh sách top N users trong bảng xếp hạng theo loại (DAILY, WEEKLY, MONTHLY, ALLTIME).

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | String | ✅ Yes | Loại leaderboard: `DAILY`, `WEEKLY`, `MONTHLY`, `ALLTIME` (không phân biệt hoa thường) |
| `topNumber` | Integer | ✅ Yes | Số lượng user top muốn lấy (ví dụ: 10, 20, 50) |

**Response** (200 OK):

```json
{
  "result": [
    {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "score": 5000.0,
      "top": 1
    },
    {
      "userId": "660e8400-e29b-41d4-a716-446655440001",
      "score": 4500.0,
      "top": 2
    },
    {
      "userId": "770e8400-e29b-41d4-a716-446655440002",
      "score": 4000.0,
      "top": 3
    }
  ],
  "status": "SUCCESS"
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `result` | Array | Danh sách các entry trong leaderboard, sắp xếp theo điểm giảm dần |
| `result[].userId` | String | ID của user |
| `result[].score` | Double | Tổng điểm của user trong khoảng thời gian tương ứng |
| `result[].top` | Integer | Vị trí xếp hạng (1 = top 1, 2 = top 2, ...) |
| `status` | String | Trạng thái response (thường là "SUCCESS") |

**Leaderboard Types**:

| Type | Mô tả |
|------|-------|
| `DAILY` | Bảng xếp hạng theo ngày (từ 00:00 đến 23:59 cùng ngày) |
| `WEEKLY` | Bảng xếp hạng theo tuần (từ đầu tuần đến cuối tuần) |
| `MONTHLY` | Bảng xếp hạng theo tháng (từ đầu tháng đến cuối tháng) |
| `ALLTIME` | Bảng xếp hạng tổng thể (tất cả thời gian) |

**Error Responses**:

- **400 Bad Request**: Loại leaderboard không hợp lệ
  ```json
  {
    "result": [],
    "status": "Invalid leaderboard type. Valid types: DAILY, WEEKLY, MONTHLY, ALLTIME"
  }
  ```

- **500 Internal Server Error**: Lỗi server

**Example cURL**:

```bash
# Lấy top 10 daily leaderboard (qua Gateway)
curl -X GET http://localhost:8080/gamification/leaderboard/DAILY/10 \
  -H "Authorization: Bearer <token>"

# Lấy top 20 weekly leaderboard (trực tiếp)
curl -X GET http://localhost:8203/api/v1/gamify/leaderboard/WEEKLY/20 \
  -H "Authorization: Bearer <token>"

# Lấy top 50 all-time leaderboard
curl -X GET http://localhost:8080/gamification/leaderboard/ALLTIME/50 \
  -H "Authorization: Bearer <token>"
```

---

### 2.2. Lấy vị trí xếp hạng của user hiện tại

**Endpoint**: `GET /api/v1/gamify/leaderboard/{type}/me`

**Mô tả**: Lấy vị trí xếp hạng và điểm số của user hiện tại (từ JWT token) trong leaderboard.

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | String | ✅ Yes | Loại leaderboard: `DAILY`, `WEEKLY`, `MONTHLY`, `ALLTIME` |

**Authentication**: ✅ Required - Cần JWT token trong header `Authorization: Bearer <token>`

**Response** (200 OK):

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "score": 3500.0,
  "top": 5
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `userId` | String | ID của user (từ JWT token) |
| `score` | Double | Tổng điểm của user trong khoảng thời gian tương ứng |
| `top` | Integer | Vị trí xếp hạng (1 = top 1, -1 nếu không có trong top) |

**Error Responses**:

- **401 Unauthorized**: Không có hoặc token không hợp lệ
- **400 Bad Request**: Loại leaderboard không hợp lệ
- **500 Internal Server Error**: Lỗi server

**Example cURL**:

```bash
# Qua Gateway
curl -X GET http://localhost:8080/gamification/leaderboard/DAILY/me \
  -H "Authorization: Bearer <token>"

# Trực tiếp
curl -X GET http://localhost:8203/api/v1/gamify/leaderboard/WEEKLY/me \
  -H "Authorization: Bearer <token>"
```

---

## 3. Challenge APIs

### 3.1. Lấy danh sách tất cả challenges

**Endpoint**: `GET /api/v1/gamify/challenge`

**Mô tả**: Lấy danh sách tất cả các challenge có trong hệ thống.

**Query Parameters**: Không có

**Response** (200 OK):

```json
[
  {
    "id": "880e8400-e29b-41d4-a716-446655440000",
    "title": "Hoàn thành 5 quiz trong ngày",
    "description": "Thử thách hoàn thành 5 bài quiz trong một ngày",
    "type": "QUIZ",
    "scope": "DAILY",
    "target": "5",
    "startAt": "2025-01-15T00:00:00+07:00[Asia/Ho_Chi_Minh]",
    "endAt": "2025-01-15T23:59:59+07:00[Asia/Ho_Chi_Minh]",
    "active": true,
    "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":5}",
    "createdAt": "2025-01-10T10:00:00+07:00[Asia/Ho_Chi_Minh]",
    "updatedAt": "2025-01-10T10:00:00+07:00[Asia/Ho_Chi_Minh]"
  },
  {
    "id": "990e8400-e29b-41d4-a716-446655440001",
    "title": "Tiết kiệm 1 triệu trong tuần",
    "description": "Thử thách tiết kiệm 1 triệu đồng trong một tuần",
    "type": "EXPENSE",
    "scope": "WEEKLY",
    "target": "1000000",
    "startAt": "2025-01-13T00:00:00+07:00[Asia/Ho_Chi_Minh]",
    "endAt": "2025-01-19T23:59:59+07:00[Asia/Ho_Chi_Minh]",
    "active": true,
    "rule": "{\"eventType\":\"EXPENSE\",\"action\":\"SAVE\",\"amount\":1000000}",
    "createdAt": "2025-01-10T10:00:00+07:00[Asia/Ho_Chi_Minh]",
    "updatedAt": "2025-01-10T10:00:00+07:00[Asia/Ho_Chi_Minh]"
  }
]
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | ID của challenge |
| `title` | String | Tiêu đề challenge |
| `description` | String | Mô tả chi tiết challenge |
| `type` | String (Enum) | Loại challenge: `QUIZ`, `EXPENSE`, `GOAL`, `SCENARIO`, `STREAK`, `CUSTOM` |
| `scope` | String (Enum) | Phạm vi thời gian: `DAILY`, `WEEKLY`, `SEASONAL`, `ONEOFF` |
| `target` | String | Mục tiêu cần đạt (có thể là số lượng hoặc giá trị) |
| `startAt` | DateTime (ISO 8601) | Thời gian bắt đầu challenge (ZonedDateTime) |
| `endAt` | DateTime (ISO 8601) | Thời gian kết thúc challenge (ZonedDateTime) |
| `active` | Boolean | Trạng thái active của challenge |
| `rule` | String (JSON) | Rule JSON mô tả điều kiện để hoàn thành challenge |
| `createdAt` | DateTime (ISO 8601) | Thời gian tạo challenge (ZonedDateTime) |
| `updatedAt` | DateTime (ISO 8601) | Thời gian cập nhật gần nhất (ZonedDateTime) |

**Challenge Types**:

| Type | Mô tả |
|------|-------|
| `QUIZ` | Challenge liên quan đến quiz |
| `EXPENSE` | Challenge liên quan đến chi tiêu/tiết kiệm |
| `GOAL` | Challenge liên quan đến mục tiêu |
| `SCENARIO` | Challenge theo kịch bản |
| `STREAK` | Challenge về chuỗi ngày liên tiếp |
| `CUSTOM` | Challenge tùy chỉnh |

**Challenge Scopes**:

| Scope | Mô tả |
|-------|-------|
| `DAILY` | Challenge theo ngày |
| `WEEKLY` | Challenge theo tuần |
| `SEASONAL` | Challenge theo mùa/kỳ |
| `ONEOFF` | Challenge một lần |

**Error Responses**:

- **500 Internal Server Error**: Lỗi server

**Example cURL**:

```bash
# Qua Gateway
curl -X GET http://localhost:8080/gamification/challenge \
  -H "Authorization: Bearer <token>"

# Trực tiếp
curl -X GET http://localhost:8203/api/v1/gamify/challenge \
  -H "Authorization: Bearer <token>"
```

---

### 3.2. Tạo challenge mới

**Endpoint**: `POST /api/v1/gamify/challenge`

**Mô tả**: Tạo một challenge mới (dành cho admin).

**Request Body**:

```json
{
  "title": "Hoàn thành 10 quiz trong tuần",
  "description": "Thử thách hoàn thành 10 bài quiz trong một tuần",
  "type": "QUIZ",
  "scope": "WEEKLY",
  "target": "10",
  "startAt": "2025-01-13T00:00:00+07:00[Asia/Ho_Chi_Minh]",
  "endAt": "2025-01-19T23:59:59+07:00[Asia/Ho_Chi_Minh]",
  "active": true,
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":10}"
}
```

**Request Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | String | ✅ Yes | Tiêu đề challenge |
| `description` | String | ✅ Yes | Mô tả chi tiết |
| `type` | String (Enum) | ✅ Yes | Loại challenge: `QUIZ`, `EXPENSE`, `GOAL`, `SCENARIO`, `STREAK`, `CUSTOM` |
| `scope` | String (Enum) | ✅ Yes | Phạm vi: `DAILY`, `WEEKLY`, `SEASONAL`, `ONEOFF` |
| `target` | String | ✅ Yes | Mục tiêu cần đạt |
| `startAt` | DateTime (ISO 8601) | ✅ Yes | Thời gian bắt đầu (ZonedDateTime) |
| `endAt` | DateTime (ISO 8601) | ✅ Yes | Thời gian kết thúc (ZonedDateTime) |
| `active` | Boolean | ✅ Yes | Trạng thái active |
| `rule` | String (JSON) | ✅ Yes | Rule JSON mô tả điều kiện |

**Lưu ý**: 
- Field `id` sẽ được tự động generate, không cần truyền
- Field `createdAt` sẽ được tự động set khi tạo
- Field `updatedAt` sẽ được tự động set khi tạo/cập nhật

**Response** (200 OK):

```json
{
  "challengeId": "aa0e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS"
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `challengeId` | UUID | ID của challenge vừa tạo |
| `status` | String | Trạng thái xử lý (thường là "SUCCESS") |

**Error Responses**:

- **400 Bad Request**: Dữ liệu request không hợp lệ
- **500 Internal Server Error**: Lỗi server

**Example cURL**:

```bash
# Qua Gateway
curl -X POST http://localhost:8080/gamification/challenge \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "Hoàn thành 10 quiz trong tuần",
    "description": "Thử thách hoàn thành 10 bài quiz trong một tuần",
    "type": "QUIZ",
    "scope": "WEEKLY",
    "target": "10",
    "startAt": "2025-01-13T00:00:00+07:00[Asia/Ho_Chi_Minh]",
    "endAt": "2025-01-19T23:59:59+07:00[Asia/Ho_Chi_Minh]",
    "active": true,
    "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":10}"
  }'
```

---

### 3.3. Xóa challenge

**Endpoint**: `DELETE /api/v1/gamify/challenge/{challengeId}`

**Mô tả**: Xóa một challenge theo ID (dành cho admin).

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `challengeId` | UUID | ✅ Yes | ID của challenge cần xóa |

**Response** (200 OK):

```json
{
  "status": "SUCCESS"
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `status` | String | Trạng thái xử lý (thường là "SUCCESS") |

**Error Responses**:

- **404 Not Found**: Không tìm thấy challenge với ID tương ứng
- **500 Internal Server Error**: Lỗi server

**Example cURL**:

```bash
# Qua Gateway
curl -X DELETE http://localhost:8080/gamification/challenge/880e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>"

# Trực tiếp
curl -X DELETE http://localhost:8203/api/v1/gamify/challenge/880e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>"
```

---

## 4. Authentication & Testing

### 4.1. Test JWT Token (Debug Endpoint)

**Endpoint**: `GET /api/v1/gamify/me`

**Mô tả**: Endpoint để test và xem thông tin từ JWT token (dùng cho debugging).

**Authentication**: ✅ Required - Cần JWT token trong header `Authorization: Bearer <token>`

**Response** (200 OK):

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "scope": "USER"
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `sub` | String | Subject claim từ JWT (thường là userId) |
| `scope` | String | Scope claim từ JWT (quyền của user) |

**Example cURL**:

```bash
# Qua Gateway
curl -X GET http://localhost:8080/gamification/me \
  -H "Authorization: Bearer <token>"

# Trực tiếp
curl -X GET http://localhost:8203/api/v1/gamify/me \
  -H "Authorization: Bearer <token>"
```

---

## Error Handling

### Cấu trúc Error Response chuẩn

Tất cả các lỗi từ API sẽ trả về format tương tự:

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Chi tiết lỗi cụ thể",
  "path": "/api/v1/gamify/endpoint"
}
```

### HTTP Status Codes

| Status Code | Mô tả |
|-------------|-------|
| `200` | OK - Request thành công |
| `400` | Bad Request - Dữ liệu request không hợp lệ |
| `401` | Unauthorized - Không có hoặc token không hợp lệ |
| `404` | Not Found - Không tìm thấy resource |
| `500` | Internal Server Error - Lỗi server |

---

## Data Types

### UUID
Format: `550e8400-e29b-41d4-a716-446655440000`

### DateTime
Format: ISO 8601 với timezone (ZonedDateTime)
Ví dụ: `2025-01-15T10:30:00+07:00[Asia/Ho_Chi_Minh]`

### Boolean
`true` hoặc `false`

### JSON String
Rule field là một JSON string, cần parse khi sử dụng:
```json
"{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":5}"
```

---

## Notes cho Frontend

1. **Base URL**: 
   - Qua Gateway: `http://localhost:8080/gamification`
   - Trực tiếp: `http://localhost:8203/api/v1/gamify`

2. **Content-Type**: Luôn sử dụng `application/json` cho request body

3. **Authentication**: Hầu hết endpoints cần JWT token trong header:
   ```
   Authorization: Bearer <token>
   ```

4. **UUID Format**: Tất cả UUID phải đúng format chuẩn

5. **DateTime Format**: Sử dụng ISO 8601 format với timezone (ZonedDateTime)

6. **Error Handling**: Luôn kiểm tra status code và xử lý error response phù hợp

7. **Validation**: 
   - `userId`, `badge`, `score` là required trong RewardRequest
   - Các field trong Challenge model đều required (trừ `id`, `createdAt`, `updatedAt`)

8. **Leaderboard Type**: Không phân biệt hoa thường, nhưng nên gửi uppercase để nhất quán

9. **Challenge Rule**: Field `rule` là JSON string, cần parse khi hiển thị hoặc validate

10. **Gateway Routing**: 
    - Tất cả requests qua Gateway sẽ được rewrite: `/gamification/**` → `/api/v1/gamify/**`
    - Gateway tự động forward `Authorization` header xuống backend

---

## Changelog

### Version 1.0.0 (2025-01-15)
- Initial release
- Reward APIs: Add reward, Get user reward (với rewardDetail)
- Leaderboard APIs: Get leaderboard by type, Get my leaderboard position
- Challenge APIs: Get all, Create, Delete
- Authentication: Test endpoint `/me` để debug JWT

---

## Liên hệ

Nếu có thắc mắc hoặc cần hỗ trợ, vui lòng liên hệ team backend.
