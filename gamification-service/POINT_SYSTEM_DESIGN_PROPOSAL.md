# Đề Xuất Cơ Chế Cộng Điểm & Challenge System

## 📋 Mục Lục

1. [Cơ Chế Cộng Điểm Quiz Mới](#1-cơ-chế-cộng-điểm-quiz-mới)
2. [Cơ Chế Streak (Chuỗi Lửa)](#2-cơ-chế-streak-chuỗi-lửa)
3. [Cơ Chế Cộng Điểm Cho Challenge](#3-cơ-chế-cộng-điểm-cho-challenge)
4. [Các Challenge Thú Vị](#4-các-challenge-thú-vị)
5. [Giải Quyết Vấn Đề Gian Lận](#5-giải-quyết-vấn-đề-gian-lận)
6. [Implementation Guide](#6-implementation-guide)

---

## 1. Cơ Chế Cộng Điểm Quiz Mới

### 1.1. Thay Đổi Từ Thang Điểm 100 → Thang Điểm Tự Do

**Hiện tại**: Quiz có thang điểm 100, cộng delta score (điểm tăng so với lần trước)

**Đề xuất mới**: Quiz có thang điểm theo số câu hỏi

#### Công Thức Cộng Điểm Quiz

```
Điểm Quiz = Số câu hỏi × 10 điểm
Ví dụ:
- Quiz 2 câu → 20 điểm
- Quiz 5 câu → 50 điểm
- Quiz 10 câu → 100 điểm
```

#### Cơ Chế Cộng Điểm

**Option 1: Cộng điểm theo lần làm tốt nhất (Recommended)**
```
- Lần 1: Làm được 8/10 câu → 80 điểm → Cộng 80 điểm
- Lần 2: Làm được 6/10 câu → 60 điểm → Không cộng (không cải thiện)
- Lần 3: Làm được 9/10 câu → 90 điểm → Cộng 10 điểm (delta: 90-80)
```

**Option 2: Cộng điểm mỗi lần làm (Khuyến khích làm lại)**
```
- Lần 1: 8/10 câu → 80 điểm → Cộng 80 điểm
- Lần 2: 6/10 câu → 60 điểm → Cộng 60 điểm (tổng: 140)
- Lần 3: 9/10 câu → 90 điểm → Cộng 90 điểm (tổng: 230)
```

**Đề xuất**: **Option 1** (giữ nguyên logic hiện tại) vì:
- Tránh spam làm quiz nhiều lần để cộng điểm
- Khuyến khích cải thiện điểm số
- Công bằng hơn

### 1.2. Công Thức Tính Điểm Chi Tiết

```java
// Pseudo code
int calculateQuizPoints(int totalQuestions, int correctAnswers) {
    // Điểm tối đa = số câu × 10
    int maxPoints = totalQuestions * 10;
    
    // Điểm thực tế = (số câu đúng / tổng số câu) × maxPoints
    int actualPoints = (correctAnswers * maxPoints) / totalQuestions;
    
    return actualPoints;
}

// Ví dụ:
// Quiz 5 câu, làm đúng 4 câu:
// maxPoints = 5 × 10 = 50
// actualPoints = (4 × 50) / 5 = 40 điểm
```

### 1.3. Thay Đổi Cần Thiết

**Learning Service** cần gửi thêm thông tin:
```json
{
  "userId": "uuid",
  "sourceType": "QUIZ",
  "lessonId": "uuid",
  "enrollId": "enroll-123",
  "score": 40,  // Điểm thực tế (4/5 câu = 40 điểm)
  "totalQuestions": 5,  // NEW: Tổng số câu
  "correctAnswers": 4   // NEW: Số câu đúng
}
```

**Gamification Service** xử lý:
- Giữ nguyên logic delta score
- Có thể dùng `totalQuestions` và `correctAnswers` cho challenge rule (ví dụ: minScore theo %)

---

## 2. Cơ Chế Streak (Chuỗi Lửa)

### 2.1. Khái Niệm

**Streak**: Chuỗi ngày liên tiếp user làm ít nhất 1 bài quiz bất kỳ (không quan tâm điểm số)

**Đặc điểm**:
- Mỗi ngày chỉ cần làm 1 bài → +1 streak
- Hiển thị trên homepage với hiệu ứng
- Có thể khôi phục khi bị mất (3 lần/tháng)
- Reset về 0 nếu hết lượt khôi phục

### 2.2. Database Schema

```sql
CREATE TABLE user_streak (
    streak_id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL UNIQUE,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_activity_date DATE NOT NULL,
    restore_count_this_month INT NOT NULL DEFAULT 0,
    last_restore_month INT,  -- Tháng cuối cùng khôi phục (1-12)
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE streak_history (
    history_id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    streak_value INT NOT NULL,
    action_type ENUM('INCREASE', 'RESET', 'RESTORE') NOT NULL,
    occurred_at DATETIME NOT NULL,
    reason VARCHAR(255)
);
```

### 2.3. Logic Xử Lý Streak

#### 2.3.1. Khi User Làm Quiz

```java
@Transactional
public void updateStreak(UUID userId) {
    LocalDate today = LocalDate.now();
    UserStreak streak = getOrCreateStreak(userId);
    
    // Nếu đã làm quiz hôm nay → Không cần update
    if (streak.getLastActivityDate().equals(today)) {
        return;
    }
    
    LocalDate yesterday = today.minusDays(1);
    
    if (streak.getLastActivityDate().equals(yesterday)) {
        // Tiếp tục chuỗi
        streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        streak.setLongestStreak(Math.max(streak.getLongestStreak(), streak.getCurrentStreak()));
    } else {
        // Chuỗi bị đứt
        streak.setCurrentStreak(1); // Bắt đầu lại từ 1
    }
    
    streak.setLastActivityDate(today);
    saveStreakHistory(userId, streak.getCurrentStreak(), "INCREASE");
    save(streak);
}
```

#### 2.3.2. Khôi Phục Streak

```java
@Transactional
public StreakRestoreResponse restoreStreak(UUID userId) {
    UserStreak streak = getStreak(userId);
    LocalDate today = LocalDate.now();
    int currentMonth = today.getMonthValue();
    
    // Check lượt khôi phục
    if (streak.getLastRestoreMonth() != null && 
        streak.getLastRestoreMonth() == currentMonth) {
        // Đã khôi phục trong tháng này
        if (streak.getRestoreCountThisMonth() >= 3) {
            return new StreakRestoreResponse(false, "Đã hết lượt khôi phục trong tháng này");
        }
    } else {
        // Tháng mới → Reset counter
        streak.setRestoreCountThisMonth(0);
        streak.setLastRestoreMonth(currentMonth);
    }
    
    // Khôi phục streak
    int previousStreak = streak.getCurrentStreak();
    streak.setCurrentStreak(previousStreak + 1);
    streak.setRestoreCountThisMonth(streak.getRestoreCountThisMonth() + 1);
    streak.setLastRestoreMonth(currentMonth);
    streak.setLastActivityDate(today);
    
    saveStreakHistory(userId, streak.getCurrentStreak(), "RESTORE");
    save(streak);
    
    return new StreakRestoreResponse(true, "Khôi phục thành công");
}
```

#### 2.3.3. Reset Streak (Khi Hết Lượt Khôi Phục)

```java
@Transactional
public void resetStreakIfNeeded(UUID userId) {
    UserStreak streak = getStreak(userId);
    LocalDate today = LocalDate.now();
    LocalDate lastActivity = streak.getLastActivityDate();
    
    // Nếu quá 1 ngày và đã hết lượt khôi phục
    if (lastActivity.isBefore(today.minusDays(1)) && 
        streak.getRestoreCountThisMonth() >= 3) {
        streak.setCurrentStreak(0);
        saveStreakHistory(userId, 0, "RESET");
        save(streak);
    }
}
```

### 2.4. API Endpoints

#### 2.4.1. Lấy Streak Của User

```
GET /api/v1/gamify/streak/me
```

**Response**:
```json
{
  "code": 200,
  "result": {
    "currentStreak": 7,
    "longestStreak": 15,
    "lastActivityDate": "2025-01-15",
    "restoreCountThisMonth": 1,
    "canRestore": true
  }
}
```

#### 2.4.2. Khôi Phục Streak

```
POST /api/v1/gamify/streak/restore
```

**Response**:
```json
{
  "code": 200,
  "result": {
    "success": true,
    "newStreak": 8,
    "restoreCountRemaining": 2,
    "message": "Khôi phục thành công"
  }
}
```

### 2.5. Tích Hợp Với Quiz Completion

**Learning Service** gọi tự động khi user hoàn thành quiz:
```java
// Trong RewardService.addReward() khi sourceType = QUIZ
if (sourceType == RewardSourceType.QUIZ) {
    // ... xử lý reward ...
    
    // Update streak
    streakService.updateStreak(userId);
}
```

---

## 3. Cơ Chế Cộng Điểm Cho Challenge

### 3.1. Nguyên Tắc Cộng Điểm

**Công bằng**: Điểm thưởng phải tương xứng với độ khó và effort của challenge

**Kích thích**: Điểm thưởng đủ hấp dẫn để user muốn hoàn thành

### 3.2. Bảng Điểm Thưởng Đề Xuất

#### 3.2.1. Challenge Quiz

| Loại Challenge | Điểm Thưởng | Ví Dụ |
|----------------|-------------|-------|
| **Daily - Dễ** | 20-30 điểm | Làm 1 quiz bất kỳ |
| **Daily - Trung bình** | 40-50 điểm | Làm 3 quiz với điểm >= 70 |
| **Daily - Khó** | 60-80 điểm | Làm 5 quiz với điểm >= 90 |
| **Weekly - Dễ** | 100-150 điểm | Làm 10 quiz trong tuần |
| **Weekly - Trung bình** | 200-250 điểm | Làm 20 quiz với điểm >= 80 |
| **Weekly - Khó** | 300-400 điểm | Làm 30 quiz với điểm >= 90 |
| **Monthly** | 500-800 điểm | Làm 100 quiz trong tháng |
| **All-time** | 1000-2000 điểm | Làm 500 quiz tổng cộng |

#### 3.2.2. Challenge Finance (Thu Chi)

| Loại Challenge | Điểm Thưởng | Ví Dụ |
|----------------|-------------|-------|
| **Daily - Dễ** | 15-25 điểm | Ghi chép 1 giao dịch |
| **Daily - Trung bình** | 30-40 điểm | Ghi chép 3 giao dịch |
| **Daily - Khó** | 50-70 điểm | Tiết kiệm 50.000đ trong ngày |
| **Weekly - Dễ** | 80-120 điểm | Ghi chép 10 giao dịch |
| **Weekly - Trung bình** | 150-200 điểm | Tiết kiệm 200.000đ trong tuần |
| **Weekly - Khó** | 250-350 điểm | Tiết kiệm 500.000đ trong tuần |
| **Monthly** | 400-600 điểm | Tiết kiệm 2.000.000đ trong tháng |
| **All-time** | 800-1500 điểm | Tiết kiệm 10.000.000đ tổng cộng |

#### 3.2.3. Challenge Streak

| Loại Challenge | Điểm Thưởng | Ví Dụ |
|----------------|-------------|-------|
| **Daily** | 10 điểm | Duy trì streak (tự động) |
| **Weekly** | 100 điểm | Streak 7 ngày |
| **Monthly** | 500 điểm | Streak 30 ngày |
| **All-time** | 2000 điểm | Streak 100 ngày |

### 3.3. Công Thức Tính Điểm Thưởng

```java
// Pseudo code
int calculateChallengeReward(Challenge challenge) {
    int basePoints = 0;
    
    // Base points theo scope
    switch (challenge.getScope()) {
        case DAILY: basePoints = 20; break;
        case WEEKLY: basePoints = 100; break;
        case MONTHLY: basePoints = 500; break;
        case ONEOFF: basePoints = 1000; break;
    }
    
    // Multiplier theo độ khó
    double difficultyMultiplier = 1.0;
    if (challenge.getType() == ChallengeType.QUIZ) {
        // Challenge quiz khó hơn → điểm cao hơn
        ChallengeRule rule = parseRule(challenge.getRule());
        if (rule.getMinScore() != null && rule.getMinScore() >= 90) {
            difficultyMultiplier = 1.5; // Khó
        } else if (rule.getMinScore() != null && rule.getMinScore() >= 70) {
            difficultyMultiplier = 1.2; // Trung bình
        }
    } else if (challenge.getType() == ChallengeType.EXPENSE) {
        // Challenge tiết kiệm với số tiền lớn → điểm cao hơn
        int targetAmount = challenge.getTargetValue();
        if (targetAmount >= 1000000) {
            difficultyMultiplier = 1.5;
        } else if (targetAmount >= 500000) {
            difficultyMultiplier = 1.2;
        }
    }
    
    // Target multiplier (số lượng lớn → điểm cao hơn)
    double targetMultiplier = 1.0;
    if (challenge.getTargetValue() != null) {
        if (challenge.getTargetValue() >= 50) {
            targetMultiplier = 1.3;
        } else if (challenge.getTargetValue() >= 20) {
            targetMultiplier = 1.1;
        }
    }
    
    return (int)(basePoints * difficultyMultiplier * targetMultiplier);
}
```

---

### 3.4. Vòng Đời Challenge & Reset Chu Kỳ

| Scope        | Múi giờ | Thời điểm reset | Cron gợi ý (Asia/Ho_Chi_Minh) |
|--------------|---------|-----------------|--------------------------------|
| Daily        | GMT+7   | 00:00 hằng ngày | `0 0 0 * * *`                  |
| Weekly       | GMT+7   | 00:00 Thứ Hai   | `0 0 0 * * MON`                |
| Monthly      | GMT+7   | 00:00 ngày 1    | `0 0 0 1 * *`                  |
| All-time     | GMT+7   | Không reset     | _N/A_                          |
| Season/Custom| GMT+7   | Theo start/end  | Tắt cron, reset bằng rule      |

#### 3.4.1. Nguyên Tắc Lưu Trữ

- Mỗi cặp `userId + challengeId` có **một** bản ghi tiến trình (`user_challenge_progress`), không lưu history các chu kỳ cũ.
- Các cột chính: `progress`, `status` (`ACTIVE/COMPLETED/EXPIRED`), `rewardClaimed` (boolean), `lastUpdatedAt`, `lastResetAt`.
- Khi cron chạy đến chu kỳ mới:
  1. Set `progress = 0`, `status = ACTIVE`, `rewardClaimed = false`.
  2. Cập nhật `lastResetAt = now` để tránh reset trùng.
  3. Không xóa bản ghi → giữ lại cho lần sau.
- All-time hoặc Season challenge (có `startAt`, `endAt` rõ ràng) **không reset** trừ khi qua mùa → xử lý thủ công bằng rule `endAt`.

#### 3.4.2. Cron Service

Pseudo code:

```java
@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
public void resetDailyChallenges() {
    progressRepository.resetByScope(ChallengeScope.DAILY);
}

@Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Ho_Chi_Minh")
public void resetWeeklyChallenges() {
    progressRepository.resetByScope(ChallengeScope.WEEKLY);
}

@Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Ho_Chi_Minh")
public void resetMonthlyChallenges() {
    progressRepository.resetByScope(ChallengeScope.MONTHLY);
}
```

Các hàm `resetByScope` nên:
- Chỉ reset những challenge có `scope` tương ứng và `isResettable = true`.
- Bỏ qua challenge đang ở trạng thái `INACTIVE` hoặc `SOFT_DELETED`.

#### 3.4.3. Anti-Cheat Khi Cấp Badge & Điểm

- Trước khi cộng điểm/badge, load bản ghi `user_challenge_progress`.
- Nếu `status == COMPLETED` **và** `rewardClaimed == true` → từ chối cấp thêm.
- Nếu `status == COMPLETED` nhưng `rewardClaimed == false`: cấp điểm, tăng counter badge (`badge_daily_count`, `badge_weekly_count`, `badge_monthly_count`...), sau đó set `rewardClaimed = true`.
- Badge hoạt động dạng counter, ví dụ: user có `dailyBadgeCount = 15`, `weeklyBadgeCount = 7`. Không cần log history từng lần hoàn thành.

#### 3.4.4. Lý Do Không Lưu History

- Yêu cầu hiện tại chỉ cần trạng thái “hôm nay/tuần này/tháng này” → một record là đủ.
- Giảm dung lượng DB và đơn giản hoá API.
- Nếu sau này cần thống kê, có thể bổ sung bảng `challenge_progress_log` ghi nhận snapshot mà không ảnh hưởng kiến trúc hiện tại.

---

## 4. Các Challenge Thú Vị

### 4.1. Challenge Quiz

#### Daily Challenges

**1. "Khởi Động Ngày Mới" (Dễ)**
```json
{
  "title": "Khởi Động Ngày Mới",
  "description": "Làm 1 quiz bất kỳ để bắt đầu ngày mới",
  "type": "QUIZ",
  "scope": "DAILY",
  "targetValue": 1,
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":1}",
  "rewardScore": 20,
  "rewardBadgeCode": "DAILY_STARTER",
  "maxProgressPerDay": 1
}
```

**2. "Học Tập Chăm Chỉ" (Trung bình)**
```json
{
  "title": "Học Tập Chăm Chỉ",
  "description": "Làm 3 quiz với điểm >= 70 trong ngày",
  "type": "QUIZ",
  "scope": "DAILY",
  "targetValue": 3,
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":3,\"minScore\":70,\"maxProgressPerDay\":3}",
  "rewardScore": 50,
  "rewardBadgeCode": "DAILY_DEDICATED"
}
```

**3. "Xuất Sắc Mỗi Ngày" (Khó)**
```json
{
  "title": "Xuất Sắc Mỗi Ngày",
  "description": "Làm 5 quiz với điểm >= 90 trong ngày",
  "type": "QUIZ",
  "scope": "DAILY",
  "targetValue": 5,
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":5,\"minScore\":90,\"maxProgressPerDay\":5}",
  "rewardScore": 80,
  "rewardBadgeCode": "DAILY_EXCELLENT"
}
```

#### Weekly Challenges

**1. "Tuần Học Tập" (Dễ)**
```json
{
  "title": "Tuần Học Tập",
  "description": "Làm 10 quiz trong tuần",
  "type": "QUIZ",
  "scope": "WEEKLY",
  "targetValue": 10,
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":10}",
  "rewardScore": 120,
  "rewardBadgeCode": "WEEKLY_LEARNER"
}
```

**2. "Chuyên Nghiệp" (Trung bình)**
```json
{
  "title": "Chuyên Nghiệp",
  "description": "Làm 20 quiz với điểm >= 80 trong tuần",
  "type": "QUIZ",
  "scope": "WEEKLY",
  "targetValue": 20,
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":20,\"minScore\":80}",
  "rewardScore": 250,
  "rewardBadgeCode": "WEEKLY_PRO"
}
```

**3. "Bậc Thầy" (Khó)**
```json
{
  "title": "Bậc Thầy",
  "description": "Làm 30 quiz với điểm >= 90 trong tuần",
  "type": "QUIZ",
  "scope": "WEEKLY",
  "targetValue": 30,
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":30,\"minScore\":90}",
  "rewardScore": 400,
  "rewardBadgeCode": "WEEKLY_MASTER"
}
```

#### Monthly Challenges

**1. "Tháng Kiên Trì"**
```json
{
  "title": "Tháng Kiên Trì",
  "description": "Làm 100 quiz trong tháng",
  "type": "QUIZ",
  "scope": "MONTHLY",
  "targetValue": 100,
  "rule": "{\"eventType\":\"QUIZ\",\"action\":\"COMPLETE\",\"count\":100}",
  "rewardScore": 800,
  "rewardBadgeCode": "MONTHLY_PERSISTENT"
}
```

### 4.2. Challenge Finance (Thu Chi)

#### Daily Challenges

**1. "Ghi Chép Hàng Ngày" (Dễ)**
```json
{
  "title": "Ghi Chép Hàng Ngày",
  "description": "Ghi chép ít nhất 1 giao dịch trong ngày",
  "type": "EXPENSE",
  "scope": "DAILY",
  "targetValue": 1,
  "rule": "{\"eventType\":\"EXPENSE\",\"action\":\"RECORD\",\"count\":1,\"maxProgressPerDay\":1}",
  "rewardScore": 15,
  "rewardBadgeCode": "DAILY_TRACKER"
}
```

**2. "Tiết Kiệm Nhỏ" (Trung bình)**
```json
{
  "title": "Tiết Kiệm Nhỏ",
  "description": "Tiết kiệm 50.000đ trong ngày (thu - chi >= 50.000)",
  "type": "EXPENSE",
  "scope": "DAILY",
  "targetValue": 50000,
  "rule": "{\"eventType\":\"EXPENSE\",\"action\":\"SAVE\",\"amount\":50000}",
  "rewardScore": 50,
  "rewardBadgeCode": "DAILY_SAVER"
}
```

#### Weekly Challenges

**1. "Quản Lý Tài Chính Tuần"**
```json
{
  "title": "Quản Lý Tài Chính Tuần",
  "description": "Ghi chép 10 giao dịch trong tuần",
  "type": "EXPENSE",
  "scope": "WEEKLY",
  "targetValue": 10,
  "rule": "{\"eventType\":\"EXPENSE\",\"action\":\"RECORD\",\"count\":10}",
  "rewardScore": 100,
  "rewardBadgeCode": "WEEKLY_MANAGER"
}
```

**2. "Tiết Kiệm Tuần"**
```json
{
  "title": "Tiết Kiệm Tuần",
  "description": "Tiết kiệm 200.000đ trong tuần",
  "type": "EXPENSE",
  "scope": "WEEKLY",
  "targetValue": 200000,
  "rule": "{\"eventType\":\"EXPENSE\",\"action\":\"SAVE\",\"amount\":200000}",
  "rewardScore": 200,
  "rewardBadgeCode": "WEEKLY_SAVER"
}
```

#### Monthly Challenges

**1. "Tiết Kiệm Tháng"**
```json
{
  "title": "Tiết Kiệm Tháng",
  "description": "Tiết kiệm 2.000.000đ trong tháng",
  "type": "EXPENSE",
  "scope": "MONTHLY",
  "targetValue": 2000000,
  "rule": "{\"eventType\":\"EXPENSE\",\"action\":\"SAVE\",\"amount\":2000000}",
  "rewardScore": 600,
  "rewardBadgeCode": "MONTHLY_SAVER"
}
```

### 4.3. Challenge Streak

**1. "Tuần Kiên Trì"**
```json
{
  "title": "Tuần Kiên Trì",
  "description": "Duy trì streak 7 ngày liên tiếp",
  "type": "STREAK",
  "scope": "WEEKLY",
  "targetValue": 7,
  "rule": "{\"eventType\":\"STREAK\",\"action\":\"MAINTAIN\",\"count\":7}",
  "rewardScore": 100,
  "rewardBadgeCode": "STREAK_WEEK"
}
```

**2. "Tháng Kiên Trì"**
```json
{
  "title": "Tháng Kiên Trì",
  "description": "Duy trì streak 30 ngày liên tiếp",
  "type": "STREAK",
  "scope": "MONTHLY",
  "targetValue": 30,
  "rule": "{\"eventType\":\"STREAK\",\"action\":\"MAINTAIN\",\"count\":30}",
  "rewardScore": 500,
  "rewardBadgeCode": "STREAK_MONTH"
}
```

### 4.4. Challenge Kết Hợp (Combo)

**1. "Học Tập & Quản Lý"**
```json
{
  "title": "Học Tập & Quản Lý",
  "description": "Làm 5 quiz VÀ ghi chép 3 giao dịch trong ngày",
  "type": "CUSTOM",
  "scope": "DAILY",
  "targetValue": 2,
  "rule": "{\"eventType\":\"COMBO\",\"actions\":[{\"type\":\"QUIZ\",\"count\":5},{\"type\":\"EXPENSE\",\"count\":3}]}",
  "rewardScore": 70,
  "rewardBadgeCode": "COMBO_DAILY"
}
```

---

## 5. Giải Quyết Vấn Đề Gian Lận

### 5.1. Vấn Đề: Challenge Tiết Kiệm

**Vấn đề**: User có thể bỏ tiền vào goal để hoàn thành challenge, sau đó rút ra.

**Giải pháp**: Track "net savings" thay vì "deposit amount"

### 5.2. Cơ Chế "Net Savings"

#### 5.2.1. Công Thức

```
Net Savings = Tổng Thu Nhập - Tổng Chi Tiêu - Số Tiền Rút Ra
```

**Lưu ý**: 
- Chỉ tính trong khoảng thời gian của challenge
- Không tính số tiền rút ra từ goal (vì đã tính vào chi tiêu)
- Chỉ tính số tiền thực sự tiết kiệm được

#### 5.2.2. Implementation

**Finance Service** cần track:
```java
// Khi user rút tiền từ goal
public void withdrawFromGoal(UUID goalId, BigDecimal amount) {
    // ... xử lý rút tiền ...
    
    // Publish event để gamification service biết
    publishEvent(new FinanceEvent(
        userId,
        "EXPENSE",
        "WITHDRAW_FROM_GOAL",
        amount.negate(), // Số âm để trừ khỏi savings
        goalId
    ));
}
```

**Gamification Service** xử lý:
```java
// Track net savings cho challenge
private void updateSavingsChallenge(ChallengeEventRequest event) {
    if (event.getAction().equals("SAVE")) {
        // Cộng vào savings
        currentSavings += event.getAmount();
    } else if (event.getAction().equals("WITHDRAW_FROM_GOAL")) {
        // Trừ khỏi savings (vì rút ra = không còn tiết kiệm)
        currentSavings += event.getAmount(); // amount đã là số âm
    }
    
    // Check completion
    if (currentSavings >= targetAmount) {
        completeChallenge();
    }
}
```

### 5.3. Cơ Chế "Lock Period"

**Ý tưởng**: Sau khi hoàn thành challenge, lock số tiền trong goal trong một khoảng thời gian.

**Implementation**:
```java
// Khi challenge hoàn thành
if (challenge.getType() == ChallengeType.EXPENSE && 
    challenge.getScope() == ChallengeScope.WEEKLY) {
    
    // Lock goal trong 7 ngày
    goalService.lockGoal(goalId, 7, "Challenge completion lock");
}
```

**Nhược điểm**: Phức tạp và có thể gây khó chịu cho user.

**Đề xuất**: **Không dùng** cơ chế này, thay vào đó dùng **Net Savings**.

### 5.4. Cơ Chế "Minimum Hold Time"

**Ý tưởng**: Tiền phải được giữ trong goal ít nhất X ngày mới được tính vào challenge.

**Implementation**:
```java
// Track khi user deposit vào goal
depositHistory.add(new Deposit(goalId, amount, LocalDate.now()));

// Khi check challenge completion
for (Deposit deposit : deposits) {
    if (LocalDate.now().minusDays(7).isAfter(deposit.getDate())) {
        // Đã giữ 7 ngày → Tính vào savings
        validSavings += deposit.getAmount();
    }
}
```

**Đề xuất**: Có thể dùng cho challenge **WEEKLY** hoặc **MONTHLY** (giữ ít nhất 3-7 ngày).

### 5.5. Giải Pháp Đề Xuất (Recommended)

**Kết hợp 2 cơ chế**:

1. **Net Savings**: Tính `thu - chi - rút` trong khoảng thời gian challenge
2. **Minimum Hold Time**: Với challenge WEEKLY/MONTHLY, tiền phải được giữ ít nhất 3 ngày

**Ví dụ Challenge "Tiết Kiệm Tuần"**:
```json
{
  "title": "Tiết Kiệm Tuần",
  "description": "Tiết kiệm 200.000đ trong tuần (tiền phải được giữ ít nhất 3 ngày)",
  "type": "EXPENSE",
  "scope": "WEEKLY",
  "targetValue": 200000,
  "rule": "{\"eventType\":\"EXPENSE\",\"action\":\"SAVE\",\"amount\":200000,\"minHoldDays\":3}",
  "rewardScore": 200
}
```

**Logic**:
- User deposit 200.000đ vào goal ngày 1
- User rút 200.000đ ngày 2 → Không tính (chưa đủ 3 ngày)
- User deposit 200.000đ vào goal ngày 1, giữ đến ngày 4 → Tính vào challenge

---

## 6. Implementation Guide

### 6.1. Thay Đổi Cần Thiết

#### 6.1.1. Learning Service

**Thay đổi RewardRequest**:
```java
public class RewardRequest {
    // ... existing fields ...
    
    private Integer totalQuestions;  // NEW
    private Integer correctAnswers;  // NEW
}
```

**Gửi request**:
```java
RewardRequest request = new RewardRequest();
request.setUserId(userId);
request.setSourceType(RewardSourceType.QUIZ);
request.setLessonId(lessonId);
request.setEnrollId(enrollId);
request.setScore(calculatePoints(totalQuestions, correctAnswers));
request.setTotalQuestions(totalQuestions);  // NEW
request.setCorrectAnswers(correctAnswers); // NEW
```

#### 6.1.2. Gamification Service

**Thêm Streak Service**:
```java
@Service
public class StreakService {
    // ... implementation ...
}
```

**Thêm Finance Event Types**:
```java
// ChallengeEventRequest
- eventType: "EXPENSE"
- action: "RECORD", "SAVE", "WITHDRAW_FROM_GOAL"
- amount: số tiền (có thể âm cho withdraw)
```

**Thêm Challenge Types**:
```java
public enum ChallengeType {
    QUIZ,
    EXPENSE,
    GOAL,
    STREAK,  // NEW
    COMBO,   // NEW
    CUSTOM
}
```

### 6.2. Migration Plan

**Phase 1**: Thêm fields mới (backward compatible)
- Thêm `totalQuestions`, `correctAnswers` vào RewardRequest
- Thêm Streak tables
- Thêm Streak service

**Phase 2**: Update logic
- Update quiz point calculation
- Implement streak logic
- Implement finance challenge với net savings

**Phase 3**: Deploy challenges mới
- Tạo các challenge mới theo đề xuất
- Test và monitor

### 6.3. Testing Scenarios

**Quiz Points**:
- Quiz 2 câu, đúng 2 → 20 điểm
- Quiz 5 câu, đúng 4 → 40 điểm
- Làm lại quiz, điểm thấp hơn → Không cộng điểm

**Streak**:
- Làm quiz ngày 1 → Streak = 1
- Làm quiz ngày 2 → Streak = 2
- Không làm ngày 3 → Streak = 0
- Khôi phục → Streak = 1, restoreCount = 1
- Khôi phục lần 4 trong tháng → Fail

**Finance Challenge**:
- Deposit 200k vào goal → Savings = 200k
- Rút 200k ngày hôm sau → Savings = 0 (net)
- Deposit 200k, giữ 4 ngày → Tính vào challenge (nếu minHoldDays = 3)

---

## 7. Tổng Kết

### 7.1. Điểm Mạnh Của Đề Xuất

1. **Công bằng**: Điểm thưởng tương xứng với effort
2. **Kích thích**: Streak và challenge đa dạng
3. **Chống gian lận**: Net savings + minimum hold time
4. **Linh hoạt**: Dễ thêm challenge mới

### 7.2. Lưu Ý

1. **Balance**: Điểm thưởng không quá cao → Tránh inflation
2. **Monitor**: Theo dõi user behavior để điều chỉnh
3. **Feedback**: Thu thập feedback từ user về challenge

### 7.3. Next Steps

1. Review và approve đề xuất
2. Implement Phase 1 (backward compatible)
3. Test với một nhóm user nhỏ
4. Deploy và monitor
5. Iterate dựa trên data

---

**Tài liệu này là đề xuất ban đầu, có thể điều chỉnh dựa trên feedback và thực tế triển khai.**

