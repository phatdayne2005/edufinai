# Gamification Service - Tài Liệu API Đầy Đủ

## 📋 Mục Lục

1. [Tổng Quan Hệ Thống](#1-tổng-quan-hệ-thống)
2. [Workflow & Luồng Hoạt Động](#2-workflow--luồng-hoạt-động)
3. [API Endpoints](#3-api-endpoints)
4. [Use Cases](#4-use-cases)
5. [Integration Guide](#5-integration-guide)
6. [Data Models](#6-data-models)

---

## 1. Tổng Quan Hệ Thống

### 1.1. Giới Thiệu

Gamification Service là một microservice độc lập quản lý toàn bộ hệ thống gamification của ứng dụng, bao gồm:

- **Rewards (Phần thưởng)**: Quản lý điểm thưởng và cộng điểm cho người dùng
- **Challenges (Thử thách)**: Tạo và theo dõi tiến độ hoàn thành thử thách
- **Badges (Huy hiệu)**: Quản lý huy hiệu người dùng đạt được
- **Leaderboards (Bảng xếp hạng)**: Xếp hạng người dùng theo thời gian (Daily, Weekly, Monthly, All-time)

### 1.2. Thông Tin Kỹ Thuật

- **Service Name**: Gamification Service
- **Direct Base URL**: `http://localhost:8203/api/v1/gamify`
- **Gateway Base URL**: `http://localhost:8080/gamification`
- **Version**: 1.0.0
- **Content-Type**: `application/json`
- **Authentication**: JWT Token (Bearer Token)

**Lưu ý**: Tất cả requests qua Gateway sẽ được rewrite từ `/gamification/**` → `/api/v1/gamify/**`

> **Gateway Route (tham khảo `gateway/src/main/resources/application.yml`)**
> ```yaml
> - id: gamification-service
>   uri: lb://GAMIFICATION-SERVICE
>   predicates:
>     - Path=/gamification/**
>   filters:
>     - name: RewritePath
>       args:
>         regexp: /gamification/?(?<segment>.*)
>         replacement: /api/v1/gamify/${segment}
> ```
> Frontend/clients luôn gọi thông qua `http://localhost:8080/gamification/...`, Gateway tự định tuyến đến Gamification Service.

### 1.3. Kiến Trúc Hệ Thống

```
┌─────────────────┐
│  Learning Service│
│  (Quiz Service)  │
└────────┬─────────┘
         │
         │ POST /gamification/reward
         │ (sourceType: QUIZ)
         ▼
┌─────────────────────────────────────┐
│      Gamification Service            │
│  ┌───────────────────────────────┐   │
│  │   RewardService               │   │
│  │   - addReward()               │   │
│  │   - getUserReward()           │   │
│  └───────────┬───────────────────┘   │
│              │                        │
│  ┌───────────▼───────────────────┐   │
│  │ ChallengeProgressService       │   │
│  │ - processEvent()               │   │
│  │ - getActiveProgress()          │   │
│  └───────────┬───────────────────┘   │
│              │                        │
│  ┌───────────▼───────────────────┐   │
│  │ BadgeService                   │   │
│  │ - awardBadge()                 │   │
│  │ - getBadgesOfUser()            │   │
│  └────────────────────────────────┘   │
│                                        │
│  ┌────────────────────────────────┐   │
│  │ LeaderboardService             │   │
│  │ (Redis-based)                  │   │
│  └────────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## 2. Workflow & Luồng Hoạt Động

### 2.1. Workflow Tổng Quan

```
User Action → Service Event → Gamification Service → Reward/Challenge/Badge/Leaderboard
```

### 2.2. Workflow Chi Tiết: Quiz Completion

#### 2.2.1. Luồng Hoàn Thành Quiz

```
1. User hoàn thành Quiz
   │
   ▼
2. Learning Service gọi POST /gamification/reward
   {
     "userId": "uuid",
     "sourceType": "QUIZ",
     "lessonId": "uuid",
     "enrollId": "string",
     "score": 85
   }
   │
   ▼
3. RewardService.addReward()
   ├─ Validate lesson payload
   ├─ Process lesson attempt (check duplicate, calculate delta score)
   ├─ Publish challenge event (ChallengeEventPublisher)
   │  └─ ChallengeProgressService.processEvent()
   │     ├─ Lấy tất cả active challenges
   │     ├─ Filter challenges match với event
   │     ├─ Update progress cho mỗi challenge
   │     ├─ Check completion → Trao reward + badge nếu hoàn thành
   │     └─ Save progress
   ├─ Save reward record
   ├─ Update user reward summary
   └─ Update leaderboards (Daily, Weekly, Monthly, All-time)
   │
   ▼
4. Response: RewardResponse
   {
     "rewardId": "uuid",
     "status": "SUCCESS"
   }
```

#### 2.2.2. Luồng Challenge Event Processing

```
ChallengeEventRequest
   │
   ▼
ChallengeProgressService.processEvent()
   │
   ├─ Lấy active challenges (startAt <= now <= endAt)
   │
   ▼
   For each challenge:
   │
   ├─ Parse rule JSON → ChallengeRule
   │
   ├─ Check rule matches event
   │  ├─ eventType match?
   │  ├─ action match?
   │  ├─ minScore <= event.score?
   │  └─ event.score <= maxScore?
   │
   ├─ Get or create UserChallengeProgress
   │
   ├─ Check if already completed → Skip if true
   │
   ├─ Check daily limit (maxProgressPerDay)
   │
   ├─ Increase progress
   │  ├─ currentProgress += 1 (event.amount chỉ để log thêm metadata)
   │  └─ progressCountToday += 1
   │
   ├─ Check completion
   │  └─ If currentProgress >= targetProgress:
   │     ├─ Mark as completed
   │     ├─ Grant reward (if rewardScore > 0)
   │     └─ Award badge (if rewardBadgeCode exists)
   │
   └─ Save progress
```

### 2.3. Workflow: Manual Reward

```
Admin/System → POST /gamification/reward
   {
     "userId": "uuid",
     "sourceType": "MANUAL",
     "score": 100,
     "badge": "SPECIAL_BADGE",
     "reason": "Special achievement"
   }
   │
   ▼
RewardService.addReward()
   ├─ Save reward record
   ├─ Update user reward summary
   └─ Update leaderboards
```

### 2.4. Workflow: Challenge Completion

```
Challenge hoàn thành
   │
   ▼
completeChallenge()
   ├─ Mark progress as completed
   ├─ If rewardScore > 0:
   │  └─ RewardService.addReward()
   │     └─ sourceType: CHALLENGE
   └─ If rewardBadgeCode exists:
      └─ BadgeService.awardBadge()
```

### 2.5. Reset Challenge Theo Chu Kỳ

Gamification Service có cron job (`ChallengeResetService`) chạy trên timezone `Asia/Ho_Chi_Minh` để reset tiến trình mỗi chu kỳ:

| Scope   | Thời điểm reset | Cron | Ghi chú |
|---------|-----------------|------|---------|
| Daily   | 00:00 hằng ngày | `0 0 0 * * *` | Reset `currentProgress`, `progressCountToday`, `completed` cho tất cả daily progress còn active |
| Weekly  | 00:00 Thứ Hai   | `0 0 0 * * MON` | Áp dụng cho challenge scope WEEKLY |
| Monthly | 00:00 ngày 1    | `0 0 0 1 * *` | Áp dụng cho challenge scope MONTHLY |

- Reset chỉ ảnh hưởng các challenge `active = true` và `startAt <= now <= endAt`.
- `SEASONAL`, `ONEOFF`, `ALLTIME` không reset tự động; admin quản lý thủ công theo `startAt`/`endAt`.
- Khi reset, hệ thống giữ nguyên `userId + challengeId` record, chỉ đưa progress về 0 để tiết kiệm dung lượng (không lưu history mỗi chu kỳ).

---

## 3. API Endpoints

### 3.1. Reward APIs

#### 3.1.1. Thêm Phần Thưởng

**Endpoint**: `POST /api/v1/gamify/reward`  
**Gateway**: `POST /gamification/reward`

**Mô tả**: Thêm điểm thưởng và badge cho user. Hỗ trợ nhiều loại source:
- `QUIZ`: Từ hoàn thành quiz (tự động xử lý duplicate, delta score)
- `CHALLENGE`: Từ hoàn thành challenge
- `MANUAL`: Thủ công từ admin/system

**Request Body**:

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "score": 100,
  "sourceType": "QUIZ",
  "lessonId": "660e8400-e29b-41d4-a716-446655440001",
  "enrollId": "enroll-123",
  "badge": "QUIZ_MASTER",
  "reason": "Hoàn thành quiz với điểm cao",
  "challengeId": "770e8400-e29b-41d4-a716-446655440002"
}
```

**Request Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `userId` | UUID | ✅ Yes | ID của user nhận thưởng |
| `score` | Integer | ✅ Yes | Điểm số (raw score cho QUIZ, final score cho các loại khác) |
| `sourceType` | Enum | ❌ No | `QUIZ`, `CHALLENGE`, `MANUAL` (default: `MANUAL`) |
| `lessonId` | UUID | ⚠️ Yes* | Required nếu `sourceType = QUIZ` |
| `enrollId` | String | ⚠️ Yes* | Required nếu `sourceType = QUIZ` |
| `badge` | String | ❌ No | Tên badge (tùy chọn) |
| `reason` | String | ❌ No | Lý do trao thưởng |
| `challengeId` | UUID | ❌ No | ID challenge (nếu từ challenge) |

**Response** (200 OK):

```json
{
  "rewardId": "880e8400-e29b-41d4-a716-446655440003",
  "status": "SUCCESS"
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `rewardId` | UUID | ID của reward record |
| `status` | String | `SUCCESS`, `DUPLICATE_ATTEMPT`, `NO_SCORE_CHANGE` |

**Status Values**:
- `SUCCESS`: Thành công
- `DUPLICATE_ATTEMPT`: Quiz attempt đã tồn tại (chỉ cho QUIZ)
- `NO_SCORE_CHANGE`: Điểm không thay đổi (chỉ cho QUIZ)

**Xử Lý Đặc Biệt cho QUIZ**:
1. Validate `lessonId` và `enrollId` phải có
2. Process attempt qua `LessonScoreService`:
   - Check duplicate attempt
   - Calculate delta score (chỉ cộng điểm tăng)
3. Publish challenge event tự động
4. Chỉ cộng `deltaScore` (điểm tăng), không phải raw score

**Example cURL**:

```bash
# Quiz reward (tự động xử lý)
curl -X POST http://localhost:8080/gamification/reward \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "sourceType": "QUIZ",
    "lessonId": "660e8400-e29b-41d4-a716-446655440001",
    "enrollId": "enroll-123",
    "score": 85
  }'

# Manual reward
curl -X POST http://localhost:8080/gamification/reward \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "score": 100,
    "badge": "SPECIAL_BADGE",
    "reason": "Special achievement"
  }'
```

---

#### 3.1.2. Lấy Thông Tin Phần Thưởng Của User

**Endpoint**: `GET /api/v1/gamify/reward`  
**Gateway**: `GET /gamification/reward`

**Mô tả**: Lấy tổng điểm và chi tiết các phần thưởng của user hiện tại (từ JWT token).

**Authentication**: ✅ Required

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
      "sourceType": "QUIZ",
      "lessonId": "770e8400-e29b-41d4-a716-446655440002",
      "enrollId": "enroll-123",
      "challengeId": null,
      "createdAt": "2025-01-15T10:30:00"
    }
  ]
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `userId` | UUID | ID của user |
| `totalScore` | Double | Tổng điểm từ All-time leaderboard |
| `rewardDetail` | Array | Danh sách chi tiết các reward |

---

### 3.2. Challenge APIs

#### 3.2.1. Lấy Danh Sách Tất Cả Challenges

**Endpoint**: `GET /api/v1/gamify/challenge`  
**Gateway**: `GET /gamification/challenge`

**Mô tả**: Lấy danh sách tất cả challenges trong hệ thống.

**Response** (200 OK):

```json
[
  {
    "id": "880e8400-e29b-41d4-a716-446655440000",
    "title": "Hoàn thành 5 quiz trong ngày",
    "description": "Thử thách hoàn thành 5 bài quiz trong một ngày",
    "type": "QUIZ",
    "scope": "DAILY",
    "targetValue": 5,
    "startAt": "2025-01-15T00:00:00+07:00[Asia/Ho_Chi_Minh]",
    "endAt": "2025-01-15T23:59:59+07:00[Asia/Ho_Chi_Minh]",
    "active": true,
    "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":5,\"maxProgressPerDay\":3}",
    "rewardScore": 50,
    "rewardBadgeCode": "QUIZ_DAILY_5",
    "maxProgressPerDay": 3,
    "createdAt": "2025-01-10T10:00:00+07:00[Asia/Ho_Chi_Minh]",
    "updatedAt": "2025-01-10T10:00:00+07:00[Asia/Ho_Chi_Minh]"
  }
]
```

**Challenge Types**:
- `QUIZ`: Challenge liên quan đến quiz
- `EXPENSE`: Challenge liên quan đến chi tiêu/tiết kiệm
- `GOAL`: Challenge liên quan đến mục tiêu
- `SCENARIO`: Challenge theo kịch bản
- `STREAK`: Challenge về chuỗi ngày liên tiếp
- `CUSTOM`: Challenge tùy chỉnh

**Challenge Scopes**:
- `DAILY`: Reset mỗi ngày (00:00 GMT+7)
- `WEEKLY`: Reset mỗi Thứ Hai (00:00 GMT+7)
- `MONTHLY`: Reset ngày 1 hằng tháng
- `SEASONAL`: Theo mùa tùy chỉnh (không reset tự động)
- `ONEOFF`: Một lần, không reset
- `ALLTIME`: Tồn tại xuyên suốt, dùng để tính badge dài hạn

---

#### 3.2.2. Tạo Challenge Mới

**Endpoint**: `POST /api/v1/gamify/challenge`  
**Gateway**: `POST /gamification/challenge`

**Mô tả**: Tạo challenge mới (dành cho admin).

**Request Body**:

```json
{
  "title": "Hoàn thành 10 quiz trong tuần",
  "description": "Thử thách hoàn thành 10 bài quiz trong một tuần",
  "type": "QUIZ",
  "scope": "WEEKLY",
  "targetValue": 10,
  "startAt": "2025-01-13T00:00:00+07:00[Asia/Ho_Chi_Minh]",
  "endAt": "2025-01-19T23:59:59+07:00[Asia/Ho_Chi_Minh]",
  "active": true,
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":10,\"minScore\":70,\"maxProgressPerDay\":2}",
  "rewardScore": 100,
  "rewardBadgeCode": "QUIZ_WEEKLY_10",
  "maxProgressPerDay": 2
}
```

**Request Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | String | ✅ Yes | Tiêu đề challenge |
| `description` | String | ✅ Yes | Mô tả chi tiết |
| `type` | Enum | ✅ Yes | Loại challenge |
| `scope` | Enum | ✅ Yes | Phạm vi thời gian |
| `targetValue` | Integer | ❌ No | Mục tiêu (có thể dùng `count` trong rule) |
| `startAt` | ZonedDateTime | ✅ Yes | Thời gian bắt đầu |
| `endAt` | ZonedDateTime | ✅ Yes | Thời gian kết thúc |
| `active` | Boolean | ✅ Yes | Trạng thái active |
| `rule` | String (JSON) | ✅ Yes | Rule JSON mô tả điều kiện |
| `rewardScore` | Integer | ❌ No | Điểm thưởng khi hoàn thành |
| `rewardBadgeCode` | String | ❌ No | Badge code trao khi hoàn thành |
| `maxProgressPerDay` | Integer | ❌ No | Giới hạn progress mỗi ngày |

**Rule JSON Format**:

```json
{
  "eventType": "QUIZ",
  "action": "COMPLETE",
  "count": 10,
  "minScore": 70,
  "maxScore": 90,
  "maxProgressPerDay": 2
}
```

**Rule Fields**:
- `eventType`: Loại event (`QUIZ`, `EXPENSE`, etc.)
- `action`: Hành động (`COMPLETE`, `SAVE`, etc.)
- `count`: Số lần cần đạt (target)
- `minScore`: Điểm tối thiểu (cho QUIZ)
- `maxScore`: Điểm tối đa (cho QUIZ)
- `maxProgressPerDay`: Giới hạn progress mỗi ngày

**Response** (200 OK):

```json
{
  "challengeId": "aa0e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS"
}
```

---

#### 3.2.3. Xóa Challenge

**Endpoint**: `DELETE /api/v1/gamify/challenge/{challengeId}`  
**Gateway**: `DELETE /gamification/challenge/{challengeId}`

**Mô tả**: Xóa challenge theo ID.

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `challengeId` | UUID | ✅ Yes | ID của challenge |

**Response** (200 OK):

```json
{
  "status": "SUCCESS"
}
```

---

### 3.3. Challenge Progress APIs

#### 3.3.1. Publish Challenge Event (Service-to-Service)

**Endpoint**: `POST /api/v1/gamify/challenge/event`  
**Gateway**: `POST /gamification/challenge/event`

**Mô tả**: Endpoint để các service khác gửi event để xử lý challenge progress. Thường được gọi tự động từ `RewardService` khi `sourceType = QUIZ`, nhưng có thể gọi trực tiếp cho các event khác.

**Request Body**:

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "QUIZ",
  "action": "COMPLETE",
  "lessonId": "660e8400-e29b-41d4-a716-446655440001",
  "enrollId": "enroll-123",
  "score": 85,
  "amount": 1,
  "occurredAt": "2025-01-15T10:30:00+07:00[Asia/Ho_Chi_Minh]"
}
```

**Request Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `userId` | UUID | ✅ Yes | ID của user |
| `eventType` | String | ✅ Yes | Loại event (`QUIZ`, `EXPENSE`, etc.) |
| `action` | String | ✅ Yes | Hành động (`COMPLETE`, `SAVE`, etc.) |
| `lessonId` | UUID | ❌ No | ID lesson (cho QUIZ) |
| `enrollId` | String | ❌ No | ID enrollment (cho QUIZ) |
| `score` | Integer | ❌ No | Điểm số (cho QUIZ) |
| `amount` | Integer | ❌ No | Số lượng tăng progress (default: 1) |
| `occurredAt` | ZonedDateTime | ❌ No | Thời gian xảy ra (default: now) |

**Response** (200 OK):

```json
{
  "code": 200,
  "result": null,
  "message": "Event processed"
}
```

**Xử Lý**:
1. Lấy tất cả active challenges (startAt <= now <= endAt)
2. Với mỗi challenge:
   - Parse rule JSON
   - Check rule matches event
   - Get or create progress
   - Check if completed → Skip
   - Check daily limit
   - Increase progress
   - Check completion → Grant reward + badge nếu hoàn thành

---

#### 3.3.2. Lấy Progress Của Challenge

**Endpoint**: `GET /api/v1/gamify/challenge/{challengeId}/progress`  
**Gateway**: `GET /gamification/challenge/{challengeId}/progress`

**Mô tả**: Lấy tiến độ của một challenge cụ thể cho user hiện tại.

**Authentication**: ✅ Required

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `challengeId` | UUID | ✅ Yes | ID của challenge |

**Response** (200 OK):

```json
{
  "code": 200,
  "result": {
    "challengeId": "880e8400-e29b-41d4-a716-446655440000",
    "title": "Hoàn thành 5 quiz trong ngày",
    "currentProgress": 3,
    "targetProgress": 5,
    "completed": false,
    "completedAt": null
  },
  "message": "Progress retrieved"
}
```

**Response khi chưa có progress**:

```json
{
  "code": 200,
  "result": null,
  "message": "No progress found"
}
```

---

#### 3.3.3. Lấy Active Challenges

**Endpoint**: `GET /api/v1/gamify/challenge/me/active`  
**Gateway**: `GET /gamification/challenge/me/active`

**Mô tả**: Lấy danh sách các challenge đang active (chưa hoàn thành) của user hiện tại.

**Authentication**: ✅ Required

**Response** (200 OK):

```json
{
  "code": 200,
  "result": [
    {
      "challengeId": "880e8400-e29b-41d4-a716-446655440000",
      "title": "Hoàn thành 5 quiz trong ngày",
      "currentProgress": 3,
      "targetProgress": 5,
      "completed": false,
      "completedAt": null
    }
  ],
  "message": "Active challenges retrieved"
}
```

---

#### 3.3.4. Lấy Completed Challenges

**Endpoint**: `GET /api/v1/gamify/challenge/me/completed`  
**Gateway**: `GET /gamification/challenge/me/completed`

**Mô tả**: Lấy danh sách các challenge đã hoàn thành của user hiện tại.

**Authentication**: ✅ Required

**Response** (200 OK):

```json
{
  "code": 200,
  "result": [
    {
      "challengeId": "880e8400-e29b-41d4-a716-446655440000",
      "title": "Hoàn thành 5 quiz trong ngày",
      "currentProgress": 5,
      "targetProgress": 5,
      "completed": true,
      "completedAt": "2025-01-15T14:30:00+07:00[Asia/Ho_Chi_Minh]"
    }
  ],
  "message": "Completed challenges retrieved"
}
```

---

### 3.4. Badge APIs

#### 3.4.1. Lấy Badges Của User

**Endpoint**: `GET /api/v1/gamify/badge/me`  
**Gateway**: `GET /gamification/badge/me`

**Mô tả**: Lấy danh sách tất cả badges của user hiện tại.

**Authentication**: ✅ Required

**Response** (200 OK):

```json
{
  "code": 200,
  "result": [
    {
      "badgeCode": "QUIZ_MASTER",
      "badgeName": "Quiz Master",
      "badgeDescription": "Hoàn thành nhiều quiz",
      "badgeType": "QUIZ",
      "iconUrl": "https://example.com/badge/quiz-master.png",
      "count": 5,
      "firstEarnedAt": "2025-01-10T10:00:00",
      "lastEarnedAt": "2025-01-15T14:30:00",
      "sourceChallengeId": "880e8400-e29b-41d4-a716-446655440000"
    }
  ],
  "message": "Badges retrieved successfully"
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `badgeCode` | String | Code của badge |
| `badgeName` | String | Tên badge |
| `badgeDescription` | String | Mô tả badge |
| `badgeType` | Enum | Loại badge |
| `iconUrl` | String | URL icon |
| `count` | Integer | Số lần đạt được badge |
| `firstEarnedAt` | LocalDateTime | Lần đầu đạt được |
| `lastEarnedAt` | LocalDateTime | Lần cuối đạt được |
| `sourceChallengeId` | UUID | ID challenge (nếu từ challenge) |

---

### 3.5. Leaderboard APIs

#### 3.5.1. Lấy Bảng Xếp Hạng

**Endpoint**: `GET /api/v1/gamify/leaderboard/{type}/{topNumber}`  
**Gateway**: `GET /gamification/leaderboard/{type}/{topNumber}`

**Mô tả**: Lấy top N users trong leaderboard theo loại.

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | String | ✅ Yes | `DAILY`, `WEEKLY`, `MONTHLY`, `ALLTIME` |
| `topNumber` | Integer | ✅ Yes | Số lượng top users (ví dụ: 10, 20, 50) |

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
    }
  ],
  "status": "SUCCESS"
}
```

**Leaderboard Types**:
- `DAILY`: Bảng xếp hạng theo ngày
- `WEEKLY`: Bảng xếp hạng theo tuần
- `MONTHLY`: Bảng xếp hạng theo tháng
- `ALLTIME`: Bảng xếp hạng tổng thể

---

#### 3.5.2. Lấy Vị Trí Của User Hiện Tại

**Endpoint**: `GET /api/v1/gamify/leaderboard/{type}/me`  
**Gateway**: `GET /gamification/leaderboard/{type}/me`

**Mô tả**: Lấy vị trí xếp hạng và điểm số của user hiện tại.

**Authentication**: ✅ Required

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | String | ✅ Yes | `DAILY`, `WEEKLY`, `MONTHLY`, `ALLTIME` |

**Response** (200 OK):

```json
{
  "code": 200,
  "result": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "score": 3500.0,
    "top": 5
  },
  "message": "Success"
}
```

**Lưu ý**: `top` = -1 nếu user không có trong top.

---

## 4. Use Cases

### 4.1. Use Case 1: User Hoàn Thành Quiz

**Mô tả**: User hoàn thành một quiz và nhận điểm thưởng, đồng thời challenge progress được cập nhật.

**Luồng**:

1. **Learning Service** gọi `POST /gamification/reward` với:
   ```json
   {
     "userId": "user-123",
     "sourceType": "QUIZ",
     "lessonId": "lesson-456",
     "enrollId": "enroll-789",
     "score": 85
   }
   ```

2. **Gamification Service** xử lý:
   - Check duplicate attempt → Nếu duplicate → Return `DUPLICATE_ATTEMPT`
   - Calculate delta score (chỉ cộng điểm tăng)
   - Publish challenge event tự động
   - Save reward
   - Update leaderboards

3. **Challenge Processing**:
   - Tìm các active challenges có rule match `QUIZ` + `COMPLETE`
   - Update progress cho mỗi challenge match
   - Nếu challenge hoàn thành → Trao reward + badge

4. **Response**: `{"rewardId": "...", "status": "SUCCESS"}`

**Kết quả**:
- User nhận điểm thưởng (delta score)
- Challenge progress được cập nhật
- Leaderboard được cập nhật
- Nếu challenge hoàn thành → Nhận thêm reward + badge

---

### 4.2. Use Case 2: User Xem Active Challenges

**Mô tả**: User muốn xem các challenge đang tham gia và tiến độ.

**Luồng**:

1. **Frontend** gọi `GET /gamification/challenge/me/active`

2. **Gamification Service**:
   - Lấy user ID từ JWT token
   - Query `UserChallengeProgress` với `completed = false`
   - Return danh sách challenges với progress

3. **Response**:
   ```json
   {
     "code": 200,
     "result": [
       {
         "challengeId": "...",
         "title": "Hoàn thành 5 quiz trong ngày",
         "currentProgress": 3,
         "targetProgress": 5,
         "completed": false
       }
     ]
   }
   ```

**Kết quả**: User thấy danh sách challenges đang tham gia và tiến độ.

---

### 4.3. Use Case 3: Challenge Hoàn Thành Tự Động

**Mô tả**: User hoàn thành challenge thông qua các hành động (ví dụ: hoàn thành 5 quiz).

**Luồng**:

1. User hoàn thành quiz thứ 5 trong ngày
2. `ChallengeProgressService` xử lý event:
   - `currentProgress` = 4 → 5
   - Check: `5 >= 5` → Challenge hoàn thành
3. `completeChallenge()` được gọi:
   - Mark progress as completed
   - If `rewardScore > 0`: Gọi `RewardService.addReward()` với `sourceType = CHALLENGE`
   - If `rewardBadgeCode` exists: Gọi `BadgeService.awardBadge()`
4. User nhận reward + badge

**Kết quả**:
- Challenge được đánh dấu hoàn thành
- User nhận điểm thưởng từ challenge
- User nhận badge từ challenge

---

### 4.4. Use Case 4: Admin Tạo Challenge Mới

**Mô tả**: Admin tạo challenge mới cho users.

**Luồng**:

1. **Admin** gọi `POST /gamification/challenge`:
   ```json
   {
     "title": "Hoàn thành 10 quiz với điểm >= 80",
     "description": "...",
     "type": "QUIZ",
     "scope": "WEEKLY",
     "targetValue": 10,
     "startAt": "2025-01-13T00:00:00+07:00",
     "endAt": "2025-01-19T23:59:59+07:00",
     "active": true,
     "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":10,\"minScore\":80,\"maxProgressPerDay\":3}",
     "rewardScore": 200,
     "rewardBadgeCode": "QUIZ_EXCELLENT"
   }
   ```

2. **Gamification Service**:
   - Validate rule JSON
   - Save challenge
   - Return challenge ID

3. **Khi có event phù hợp**:
   - Challenge tự động được xử lý
   - Progress được cập nhật
   - Khi hoàn thành → Trao reward + badge

**Kết quả**: Challenge được tạo và tự động xử lý khi có events phù hợp.

---

### 4.5. Use Case 5: User Xem Leaderboard

**Mô tả**: User muốn xem bảng xếp hạng và vị trí của mình.

**Luồng**:

1. **Frontend** gọi:
   - `GET /gamification/leaderboard/DAILY/10` → Top 10 daily
   - `GET /gamification/leaderboard/DAILY/me` → Vị trí của user

2. **Gamification Service**:
   - Query Redis sorted set
   - Return top N users
   - Return user's position

3. **Response**:
   ```json
   {
     "result": [
       {"userId": "...", "score": 5000.0, "top": 1},
       ...
     ]
   }
   ```

**Kết quả**: User thấy bảng xếp hạng và vị trí của mình.

---

## 5. Integration Guide

> **Base URL khi tích hợp:** luôn gọi Gateway `http://localhost:8080/gamification/...`. Gateway rewrite sang `/api/v1/gamify/**` giống cấu hình ở phần 1.2, nên backend nội bộ không nên được gọi trực tiếp.

### 5.1. Integration với Learning Service

**Khi User Hoàn Thành Quiz**:

```java
// Learning Service
@PostMapping("/quiz/complete")
public ResponseEntity<QuizResult> completeQuiz(@RequestBody QuizCompletionRequest req) {
    // ... xử lý quiz ...
    
    // Gọi Gamification Service
    RewardRequest rewardRequest = new RewardRequest();
    rewardRequest.setUserId(userId);
    rewardRequest.setSourceType(RewardSourceType.QUIZ);
    rewardRequest.setLessonId(lessonId);
    rewardRequest.setEnrollId(enrollId);
    rewardRequest.setTotalQuestions(result.getTotalQuestions());
    rewardRequest.setCorrectAnswers(result.getCorrectAnswers());
    
    restTemplate.postForEntity(
        "http://gamification-service/api/v1/gamify/reward",
        rewardRequest,
        RewardResponse.class
    );
    
    return ResponseEntity.ok(quizResult);
}
```

**Lưu ý**:
- `RewardService` tự động publish challenge event khi `sourceType = QUIZ`
- Không cần gọi `/challenge/event` riêng

---

### 5.2. Integration với Finance Service

**Khi User Tiết Kiệm Tiền** (ví dụ):

```java
// Finance Service
@PostMapping("/transaction/save")
public ResponseEntity<TransactionResponse> saveMoney(@RequestBody SaveRequest req) {
    // ... xử lý transaction ...
    
    // Gọi Gamification Service để publish event
    ChallengeEventRequest event = new ChallengeEventRequest();
    event.setUserId(userId);
    event.setEventType("EXPENSE");
    event.setAction("SAVE");
    event.setAmount(amount);
    
    restTemplate.postForEntity(
        "http://gamification-service/api/v1/gamify/challenge/event",
        event,
        ApiResponse.class
    );
    
    return ResponseEntity.ok(transactionResponse);
}
```

---

### 5.3. Frontend Integration

**Lấy Active Challenges**:

```javascript
// Frontend
async function getActiveChallenges() {
  const response = await fetch('http://localhost:8080/gamification/challenge/me/active', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const data = await response.json();
  return data.result; // Array of ChallengeProgressResponse
}
```

**Lấy Leaderboard**:

```javascript
async function getLeaderboard(type, topNumber) {
  const response = await fetch(
    `http://localhost:8080/gamification/leaderboard/${type}/${topNumber}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  const data = await response.json();
  return data.result; // Array of LeaderboardEntry
}
```

### 5.4. Integration với AI Service (Tóm tắt thử thách)

**Khi AI cần gợi ý thử thách đang làm của user**:

```http
GET /api/challenges/summary
Authorization: Bearer <JWT_TOKEN>
```

**Response**:

```json
{
  "challenges": [
    {
      "challengeId": "9a2d1e0e-6a4f-4c08-9d8f-ffef8b2d0eba",
      "content": "Hoàn thành 5 bài học trong tuần",
      "progress": 60.0
    }
  ],
  "totalCount": 25
}
```

- `content`: tiêu đề challenge.
- `progress`: % hoàn thành (làm tròn 1 chữ số thập phân).
- `totalCount`: tổng số challenge đang có trong hệ thống (để AI biết quy mô, nếu cần random).

AI service chỉ cần forward JWT của user hiện tại qua header `Authorization`.

---

## 6. Data Models

### 6.1. Challenge Model

```java
{
  "id": UUID,
  "title": String,
  "description": String,
  "type": ChallengeType (QUIZ, EXPENSE, GOAL, SCENARIO, STREAK, CUSTOM),
  "scope": ChallengeScope (DAILY, WEEKLY, MONTHLY, SEASONAL, ONEOFF, ALLTIME),
  "targetValue": Integer,
  "startAt": ZonedDateTime,
  "endAt": ZonedDateTime,
  "active": Boolean,
  "rule": String (JSON),
  "rewardScore": Integer,
  "rewardBadgeCode": String,
  "maxProgressPerDay": Integer,
  "createdAt": ZonedDateTime,
  "updatedAt": ZonedDateTime
}
```

### 6.2. ChallengeRule Model

```json
{
  "eventType": "QUIZ",
  "action": "COMPLETE",
  "count": 10,
  "minAccuracy": 80,
  "maxScore": 90,
  "maxProgressPerDay": 2
}
```

### 6.3. ChallengeEventRequest Model

```json
{
  "userId": UUID,
  "eventType": "QUIZ",
  "action": "COMPLETE",
  "lessonId": UUID,
  "enrollId": String,
  "score": Integer,
  "accuracyPercent": Integer,
  "totalQuestions": Integer,
  "correctAnswers": Integer,
  "amount": Integer,
  "occurredAt": ZonedDateTime
}
```

### 6.4. RewardRequest Model

```json
{
  "userId": UUID,
  "sourceType": "QUIZ" | "CHALLENGE" | "MANUAL",
  "lessonId": UUID,
  "enrollId": String,
  "totalQuestions": Integer,
  "correctAnswers": Integer,
  "score": Integer,
  "challengeId": UUID,
  "badge": String,
  "reason": String
}
```

### 6.5. ChallengeProgressResponse Model
### 6.6. ChallengeSummaryResponse Model

```json
{
  "challenges": [
    {
      "challengeId": "UUID",
      "content": "String",
      "progress": 75.5
    }
  ],
  "totalCount": 25
}
```


```json
{
  "challengeId": UUID,
  "title": String,
  "currentProgress": Integer,
  "targetProgress": Integer,
  "completed": Boolean,
  "completedAt": ZonedDateTime
}
```

---

## 7. Cơ Chế Check Rule & Daily Limit

### 7.1. Cơ Chế Check Rule Chi Tiết

Hệ thống sử dụng **2 bước check** để xác định xem event có match với challenge không:

#### Bước 1: Rule Matching (ChallengeRuleEvaluator.matches())

Check xem event có match với rule của challenge không:

```java
// Ví dụ rule:
{
  "eventType": "QUIZ",
  "action": "COMPLETE",
  "minScore": 70,
  "maxScore": 90,
  "count": 5,
  "maxProgressPerDay": 1
}

// Check logic:
1. eventType match? → event.eventType == "QUIZ" ✅
2. action match? → event.action == "COMPLETE" ✅
3. minScore check? → event.score >= 70 ✅
4. maxScore check? → event.score <= 90 ✅
```

**Nếu tất cả đều pass → Rule match → Tiếp tục Bước 2**

#### Bước 2: Daily Limit Check (canIncrease())

Check xem có thể tăng progress không (giới hạn mỗi ngày):

```java
private boolean canIncrease(UserChallengeProgress progress, Challenge challenge, ChallengeRule rule) {
    // 1. Lấy maxProgressPerDay từ Challenge hoặc Rule
    Integer maxPerDay = challenge.getMaxProgressPerDay();
    if ((maxPerDay == null || maxPerDay <= 0) && rule != null) {
        maxPerDay = rule.getMaxProgressPerDay(); // Fallback to rule
    }
    
    // 2. Nếu không có giới hạn → Cho phép
    if (maxPerDay == null || maxPerDay <= 0) {
        return true; // Không giới hạn
    }
    
    // 3. Check ngày hiện tại
    LocalDate today = LocalDate.now();
    
    // 4. Nếu là ngày mới → Reset counter
    if (progress.getLastProgressDate() == null || 
        !progress.getLastProgressDate().equals(today)) {
        progress.setProgressCountToday(0);  // Reset về 0
        progress.setLastProgressDate(today); // Update ngày
    }
    
    // 5. Check xem đã đạt limit chưa
    return progress.getProgressCountToday() < maxPerDay;
}
```

### 7.2. Ví Dụ Cụ Thể: "1 Quiz 1 Ngày"

**Challenge Setup**:
```json
{
  "title": "Hoàn thành 1 quiz mỗi ngày",
  "type": "QUIZ",
  "scope": "DAILY",
  "targetValue": 7,
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":7,\"maxProgressPerDay\":1}",
  "maxProgressPerDay": 1
}
```

**Flow xử lý**:

**Ngày 1 - Quiz lần 1**:
```
1. Event đến: {eventType: "QUIZ", action: "COMPLETE", score: 85}
   │
   ▼
2. Rule matching:
   - eventType = "QUIZ" ✅
   - action = "COMPLETE" ✅
   - minScore/maxScore: không có → pass ✅
   │
   ▼
3. canIncrease() check:
   - maxProgressPerDay = 1
   - lastProgressDate = null (chưa có)
   - progressCountToday = 0
   - → Reset: progressCountToday = 0, lastProgressDate = 2025-01-15
   - → Check: 0 < 1 ✅ → Cho phép
   │
   ▼
4. Tăng progress:
   - currentProgress: 0 → 1
   - progressCountToday: 0 → 1
   - lastProgressDate: 2025-01-15
```

**Ngày 1 - Quiz lần 2** (cùng ngày):
```
1. Event đến: {eventType: "QUIZ", action: "COMPLETE", score: 80}
   │
   ▼
2. Rule matching: ✅ (giống trên)
   │
   ▼
3. canIncrease() check:
   - maxProgressPerDay = 1
   - lastProgressDate = 2025-01-15
   - progressCountToday = 1
   - → Check: 1 < 1 ❌ → KHÔNG cho phép
   │
   ▼
4. Return early → Không tăng progress
```

**Ngày 2 - Quiz lần 1**:
```
1. Event đến: {eventType: "QUIZ", action: "COMPLETE", score: 90}
   │
   ▼
2. Rule matching: ✅
   │
   ▼
3. canIncrease() check:
   - maxProgressPerDay = 1
   - lastProgressDate = 2025-01-15 (ngày cũ)
   - progressCountToday = 1
   - → lastProgressDate != today (2025-01-16)
   - → Reset: progressCountToday = 0, lastProgressDate = 2025-01-16
   - → Check: 0 < 1 ✅ → Cho phép
   │
   ▼
4. Tăng progress:
   - currentProgress: 1 → 2
   - progressCountToday: 0 → 1
   - lastProgressDate: 2025-01-16
```

### 7.3. Các Trường Hợp Đặc Biệt

#### Trường hợp 1: Không có maxProgressPerDay

```json
{
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":10}"
  // Không có maxProgressPerDay
}
```

→ `canIncrease()` return `true` → Không giới hạn mỗi ngày

#### Trường hợp 2: maxProgressPerDay trong Rule

```json
{
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":10,\"maxProgressPerDay\":3}",
  "maxProgressPerDay": null  // Challenge không có
}
```

→ Lấy từ Rule: `maxProgressPerDay = 3`

#### Trường hợp 3: maxProgressPerDay trong Challenge

```json
{
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":10}",
  "maxProgressPerDay": 2  // Challenge có
}
```

→ Lấy từ Challenge: `maxProgressPerDay = 2`

**Priority**: Challenge.maxProgressPerDay > Rule.maxProgressPerDay

### 7.4. Database Schema

**UserChallengeProgress** lưu trữ:

```sql
CREATE TABLE user_challenge_progress (
    progress_id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    challenge_id BINARY(16) NOT NULL,
    current_progress INT NOT NULL,
    target_progress INT NOT NULL,
    completed BOOLEAN NOT NULL,
    completed_at DATETIME,
    last_progress_date DATE,        -- Ngày cuối cùng tăng progress
    progress_count_today INT NOT NULL, -- Số lần tăng progress hôm nay
    started_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_challenge (user_id, challenge_id)
);
```

**Cơ chế reset daily counter**:
- Khi `lastProgressDate != today` → Tự động reset `progressCountToday = 0`
- Không cần scheduled job để reset (tự động khi có event mới)

### 7.5. Lưu Ý Quan Trọng

1. **Timezone**: Sử dụng `LocalDate.now()` → Theo timezone của server
2. **Reset tự động**: Reset counter khi có event mới trong ngày mới
3. **Thread-safe**: Method `canIncrease()` được gọi trong transaction → Đảm bảo consistency
4. **Edge case**: Nếu user làm quiz lúc 23:59 và quiz thứ 2 lúc 00:01 → Cả 2 đều được tính (2 ngày khác nhau)

---

## 8. Best Practices & Lưu Ý

### 8.1. Challenge Rule Design

**✅ Nên**:
- Sử dụng `maxProgressPerDay` để tránh spam
- Validate rule JSON trước khi tạo challenge
- Sử dụng `targetValue` hoặc `count` trong rule một cách nhất quán

**❌ Không nên**:
- Tạo challenge với rule không hợp lệ
- Để `maxProgressPerDay` quá cao (dễ bị abuse)

### 7.2. Event Publishing

**✅ Nên**:
- Gọi `/reward` với `sourceType = QUIZ` thay vì gọi `/challenge/event` riêng (tự động publish)
- Gọi `/challenge/event` cho các event không phải QUIZ

**❌ Không nên**:
- Gọi cả `/reward` và `/challenge/event` cho cùng một quiz completion (duplicate)

### 7.3. Leaderboard

**✅ Nên**:
- Sử dụng Redis sorted set (đã implement)
- Reset daily/weekly/monthly leaderboards theo schedule

**❌ Không nên**:
- Query leaderboard quá thường xuyên (có thể cache)

### 7.4. Error Handling

**Các Status Codes**:
- `200`: Success
- `400`: Bad Request (validation error)
- `401`: Unauthorized (missing/invalid token)
- `404`: Not Found
- `500`: Internal Server Error

**Reward Status**:
- `SUCCESS`: Thành công
- `DUPLICATE_ATTEMPT`: Quiz attempt đã tồn tại
- `NO_SCORE_CHANGE`: Điểm không thay đổi

---

## 8. Changelog

### Version 1.0.0 (2025-01-15)
- ✅ Reward APIs: Add reward, Get user reward
- ✅ Challenge APIs: Get all, Create, Delete
- ✅ Challenge Progress APIs: Publish event, Get progress, Get active/completed
- ✅ Badge APIs: Get user badges
- ✅ Leaderboard APIs: Get leaderboard, Get my position
- ✅ Challenge rule evaluation system
- ✅ Daily progress limit support
- ✅ Automatic challenge completion & reward distribution

---

## 9. Liên Hệ & Hỗ Trợ

Nếu có thắc mắc hoặc cần hỗ trợ, vui lòng liên hệ team backend.

**Tài liệu liên quan**:
- `API_DOCUMENTATION.md`: Tài liệu API cơ bản
- `CHALLENGE_ANALYSIS_AND_ENHANCEMENT.md`: Phân tích và cải tiến challenge system

