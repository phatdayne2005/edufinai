# Phân Tích và Đề Xuất Cải Thiện Challenge System

## 📊 Tình Trạng Hiện Tại

### ✅ Những gì đã có:

#### 1. Challenge Model (Challenge.java)
```java
- id: UUID
- title: String
- description: String
- type: ChallengeType (QUIZ, EXPENSE, GOAL, SCENARIO, STREAK, CUSTOM)
- scope: ChallengeScope (DAILY, WEEKLY, SEASONAL, ONEOFF)
- target: String (mục tiêu cần đạt - hiện tại là String, nên là số)
- startAt: ZonedDateTime
- endAt: ZonedDateTime
- active: boolean
- rule: String (JSON string - chưa có parser/evaluator)
- createdAt: ZonedDateTime
- updatedAt: ZonedDateTime
```

#### 2. ChallengeService
- ✅ CRUD cơ bản: findAll(), save(), delete(), findById()
- ❌ Không có logic xử lý rule
- ❌ Không có logic check progress
- ❌ Không có logic evaluate challenge completion

#### 3. ChallengeController
- ✅ GET /challenge - Lấy tất cả challenges
- ✅ POST /challenge - Tạo challenge mới
- ✅ DELETE /challenge/{id} - Xóa challenge
- ❌ Không có API để update progress
- ❌ Không có API để get user's challenge progress

---

## ❌ Những gì còn thiếu:

### 1. Challenge Model - Thiếu các thuộc tính quan trọng

#### 1.1. Reward Information
- **rewardScore**: Integer - Điểm thưởng khi hoàn thành challenge
- **rewardBadge**: String - Badge nhận được khi hoàn thành
- **rewardReason**: String - Lý do trao thưởng

#### 1.2. Progress Limitation
- **maxProgressPerDay**: Integer - Số lần tối đa có thể increase progress mỗi ngày
- **maxProgressPerWeek**: Integer (optional) - Cho weekly challenges
- **cooldownMinutes**: Integer (optional) - Thời gian chờ giữa các lần increase

#### 1.3. Rule Configuration
- **rule** hiện tại là String JSON, cần:
  - Rule parser/evaluator
  - Rule validation
  - Rule structure definition

#### 1.4. Additional Metadata
- **priority**: Integer - Độ ưu tiên hiển thị
- **imageUrl**: String - Ảnh đại diện challenge
- **icon**: String - Icon name
- **difficulty**: Enum (EASY, MEDIUM, HARD) - Độ khó

### 2. Challenge Progress Tracking - HOÀN TOÀN THIẾU

#### 2.1. UserChallengeProgress Model (Cần tạo mới)
```java
@Entity
@Table(name = "user_challenge_progress")
public class UserChallengeProgress {
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "challenge_id", nullable = false)
    private UUID challengeId;
    
    @Column(name = "current_progress", nullable = false)
    private Integer currentProgress = 0;
    
    @Column(name = "target_progress", nullable = false)
    private Integer targetProgress;
    
    @Column(name = "completed", nullable = false)
    private Boolean completed = false;
    
    @Column(name = "completed_at")
    private ZonedDateTime completedAt;
    
    @Column(name = "last_progress_date") // Để track daily limit
    private LocalDate lastProgressDate;
    
    @Column(name = "progress_count_today") // Số lần increase hôm nay
    private Integer progressCountToday = 0;
    
    @Column(name = "started_at")
    private ZonedDateTime startedAt;
    
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", insertable = false, updatable = false)
    private Challenge challenge;
}
```

#### 2.2. Challenge Progress Repository
```java
@Repository
public interface UserChallengeProgressRepository extends JpaRepository<UserChallengeProgress, UUID> {
    List<UserChallengeProgress> findByUserIdAndCompletedFalse(UUID userId);
    Optional<UserChallengeProgress> findByUserIdAndChallengeId(UUID userId, UUID challengeId);
    List<UserChallengeProgress> findByChallengeIdAndCompletedTrue(UUID challengeId);
    // Query để reset daily count
    @Modifying
    @Query("UPDATE UserChallengeProgress ucp SET ucp.progressCountToday = 0, ucp.lastProgressDate = :date WHERE ucp.lastProgressDate < :date")
    void resetDailyProgressCounts(LocalDate date);
}
```

### 3. Rule Evaluation System - HOÀN TOÀN THIẾU

#### 3.1. Rule Structure Definition
```java
// ChallengeRule.java - DTO để parse rule JSON
public class ChallengeRule {
    private String eventType; // QUIZ, EXPENSE, GOAL, etc.
    private String action; // COMPLETE, SCORE, COUNT, etc.
    private Integer count; // Số lượng cần đạt
    private Integer minScore; // Điểm tối thiểu (cho quiz)
    private Integer maxScore; // Điểm tối đa (optional)
    private Map<String, Object> conditions; // Điều kiện bổ sung
    
    // Getters, setters
}
```

#### 3.2. Rule Evaluator Service
```java
@Service
public class ChallengeRuleEvaluator {
    
    /**
     * Parse rule JSON string thành ChallengeRule object
     */
    public ChallengeRule parseRule(String ruleJson) {
        // Parse JSON string
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(ruleJson, ChallengeRule.class);
    }
    
    /**
     * Kiểm tra event có match với rule không
     */
    public boolean evaluateRule(ChallengeRule rule, ChallengeEvent event) {
        // 1. Check eventType
        if (!rule.getEventType().equals(event.getEventType())) {
            return false;
        }
        
        // 2. Check action
        if (!rule.getAction().equals(event.getAction())) {
            return false;
        }
        
        // 3. Check conditions (score, count, etc.)
        switch (rule.getEventType()) {
            case "QUIZ":
                return evaluateQuizRule(rule, event);
            case "EXPENSE":
                return evaluateExpenseRule(rule, event);
            // ... other types
        }
        
        return false;
    }
    
    private boolean evaluateQuizRule(ChallengeRule rule, ChallengeEvent event) {
        // Check minScore
        if (rule.getMinScore() != null) {
            Integer eventScore = event.getScore();
            if (eventScore == null || eventScore < rule.getMinScore()) {
                return false;
            }
        }
        
        // Check maxScore
        if (rule.getMaxScore() != null) {
            Integer eventScore = event.getScore();
            if (eventScore != null && eventScore > rule.getMaxScore()) {
                return false;
            }
        }
        
        return true;
    }
}
```

#### 3.3. Challenge Event Model
```java
// ChallengeEvent.java - Event từ các service khác
public class ChallengeEvent {
    private String eventType; // QUIZ, EXPENSE, etc.
    private String action; // COMPLETE, SCORE, etc.
    private UUID userId;
    private UUID entityId; // quizId, expenseId, etc.
    private Integer score; // Điểm số (cho quiz)
    private Integer count; // Số lượng
    private Map<String, Object> metadata; // Thông tin bổ sung
    private ZonedDateTime occurredAt;
}
```

### 4. Challenge Progress Service - HOÀN TOÀN THIẾU

#### 4.1. ChallengeProgressService
```java
@Service
@Transactional
public class ChallengeProgressService {
    private final UserChallengeProgressRepository progressRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeRuleEvaluator ruleEvaluator;
    private final RewardService rewardService;
    
    /**
     * Xử lý event và update progress cho các challenges liên quan
     */
    public void processEvent(ChallengeEvent event) {
        // 1. Lấy tất cả active challenges
        List<Challenge> activeChallenges = challengeRepository
            .findByActiveTrueAndStartAtBeforeAndEndAtAfter(
                ZonedDateTime.now(), 
                ZonedDateTime.now()
            );
        
        // 2. Filter challenges match với event
        List<Challenge> matchingChallenges = activeChallenges.stream()
            .filter(challenge -> {
                ChallengeRule rule = ruleEvaluator.parseRule(challenge.getRule());
                return ruleEvaluator.evaluateRule(rule, event);
            })
            .collect(Collectors.toList());
        
        // 3. Update progress cho từng challenge
        for (Challenge challenge : matchingChallenges) {
            updateProgress(event.getUserId(), challenge, event);
        }
    }
    
    /**
     * Update progress cho một challenge
     */
    private void updateProgress(UUID userId, Challenge challenge, ChallengeEvent event) {
        // 1. Lấy hoặc tạo progress record
        UserChallengeProgress progress = progressRepository
            .findByUserIdAndChallengeId(userId, challenge.getId())
            .orElseGet(() -> createNewProgress(userId, challenge));
        
        // 2. Check daily limit
        if (!canIncreaseProgress(progress, challenge)) {
            log.warn("Daily limit reached for user {} challenge {}", userId, challenge.getId());
            return;
        }
        
        // 3. Check đã completed chưa
        if (progress.getCompleted()) {
            return;
        }
        
        // 4. Increase progress
        int newProgress = progress.getCurrentProgress() + 1;
        progress.setCurrentProgress(newProgress);
        progress.setProgressCountToday(progress.getProgressCountToday() + 1);
        progress.setLastProgressDate(LocalDate.now());
        progress.setUpdatedAt(ZonedDateTime.now());
        
        // 5. Check completion
        if (newProgress >= progress.getTargetProgress()) {
            completeChallenge(progress, challenge);
        }
        
        progressRepository.save(progress);
    }
    
    /**
     * Kiểm tra có thể increase progress không (daily limit)
     */
    private boolean canIncreaseProgress(UserChallengeProgress progress, Challenge challenge) {
        LocalDate today = LocalDate.now();
        
        // Reset daily count nếu là ngày mới
        if (!today.equals(progress.getLastProgressDate())) {
            progress.setProgressCountToday(0);
            progress.setLastProgressDate(today);
        }
        
        // Check daily limit
        Integer maxPerDay = challenge.getMaxProgressPerDay();
        if (maxPerDay != null && progress.getProgressCountToday() >= maxPerDay) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Hoàn thành challenge và trao thưởng
     */
    private void completeChallenge(UserChallengeProgress progress, Challenge challenge) {
        progress.setCompleted(true);
        progress.setCompletedAt(ZonedDateTime.now());
        
        // Trao thưởng nếu có
        if (challenge.getRewardScore() != null && challenge.getRewardScore() > 0) {
            RewardRequest rewardRequest = new RewardRequest();
            rewardRequest.setUserId(progress.getUserId());
            rewardRequest.setScore(challenge.getRewardScore());
            rewardRequest.setBadge(challenge.getRewardBadge());
            rewardRequest.setReason("Hoàn thành challenge: " + challenge.getTitle());
            
            rewardService.addReward(rewardRequest);
        }
    }
}
```

### 5. API Endpoints - Thiếu nhiều

#### 5.1. Challenge Progress APIs
```java
// GET /api/v1/gamify/challenge/{challengeId}/progress
// Lấy progress của user hiện tại với challenge

// POST /api/v1/gamify/challenge/event
// Nhận event từ các service khác (quiz-service, expense-service, etc.)

// GET /api/v1/gamify/challenge/me/active
// Lấy danh sách challenges đang active của user với progress

// GET /api/v1/gamify/challenge/me/completed
// Lấy danh sách challenges đã hoàn thành
```

### 6. Integration với các Service khác - THIẾU

#### 6.1. Event Listener/Webhook
- Cần API để nhận events từ quiz-service khi quiz completed
- Cần API để nhận events từ expense-service khi có expense
- Có thể dùng message queue (RabbitMQ, Kafka) hoặc HTTP webhook

---

## 🎯 Các Phần Cần Bổ Sung (Theo Ưu Tiên)

### Priority 1 - Bắt Buộc (Core Functionality)

#### 1.1. Cải thiện Challenge Model
- [ ] Thêm `rewardScore`, `rewardBadge`, `rewardReason`
- [ ] Thêm `maxProgressPerDay`
- [ ] Đổi `target` từ String sang Integer
- [ ] Thêm validation cho rule format

#### 1.2. Tạo UserChallengeProgress Model
- [ ] Entity class
- [ ] Repository với các query methods cần thiết
- [ ] Database migration script

#### 1.3. Tạo Rule Evaluation System
- [ ] ChallengeRule DTO class
- [ ] ChallengeRuleEvaluator service
- [ ] ChallengeEvent model
- [ ] Rule validation logic

#### 1.4. Tạo ChallengeProgressService
- [ ] processEvent() method
- [ ] updateProgress() method
- [ ] canIncreaseProgress() - check daily limit
- [ ] completeChallenge() - trao thưởng khi hoàn thành

#### 1.5. API Endpoints
- [ ] POST /api/v1/gamify/challenge/event - Nhận events
- [ ] GET /api/v1/gamify/challenge/{id}/progress - Get progress
- [ ] GET /api/v1/gamify/challenge/me/active - Get active challenges

### Priority 2 - Quan Trọng (Enhanced Features)

#### 2.1. Rule Types cho Quiz
- [ ] Rule: Điểm số tối thiểu
  ```json
  {
    "eventType": "QUIZ",
    "action": "COMPLETE",
    "minScore": 80,
    "count": 5
  }
  ```
- [ ] Rule: Điểm số trong khoảng
  ```json
  {
    "eventType": "QUIZ",
    "action": "COMPLETE",
    "minScore": 70,
    "maxScore": 90,
    "count": 3
  }
  ```
- [ ] Rule: Số lần increase mỗi ngày
  ```json
  {
    "eventType": "QUIZ",
    "action": "COMPLETE",
    "count": 10,
    "maxProgressPerDay": 3
  }
  ```

#### 2.2. Challenge Progress Tracking
- [ ] Scheduled job để reset daily progress counts
- [ ] Progress history tracking
- [ ] Milestone notifications (50%, 75%, 100%)

#### 2.3. Challenge Filtering & Query
- [ ] Filter by type, scope, active status
- [ ] Query challenges by user progress
- [ ] Get recommended challenges

### Priority 3 - Nice to Have (Optimization)

#### 3.1. Performance Optimization
- [ ] Cache active challenges trong Redis
- [ ] Batch process events
- [ ] Async processing cho progress updates

#### 3.2. Advanced Features
- [ ] Challenge templates
- [ ] Challenge categories/tags
- [ ] Challenge difficulty levels
- [ ] Challenge images/icons

---

## 📋 Rule Examples cho Quiz

### Example 1: Hoàn thành 5 quiz với điểm >= 80
```json
{
  "eventType": "QUIZ",
  "action": "COMPLETE",
  "minScore": 80,
  "count": 5,
  "maxProgressPerDay": 2
}
```

### Example 2: Hoàn thành 10 quiz bất kỳ (không cần điểm)
```json
{
  "eventType": "QUIZ",
  "action": "COMPLETE",
  "count": 10,
  "maxProgressPerDay": 3
}
```

### Example 3: Hoàn thành 3 quiz với điểm từ 70-90
```json
{
  "eventType": "QUIZ",
  "action": "COMPLETE",
  "minScore": 70,
  "maxScore": 90,
  "count": 3,
  "maxProgressPerDay": 1
}
```

### Example 4: Hoàn thành quiz với điểm >= 90 (không giới hạn số lần/ngày)
```json
{
  "eventType": "QUIZ",
  "action": "COMPLETE",
  "minScore": 90,
  "count": 1
}
```

---

## 🔄 Flow Hoàn Chỉnh

### 1. User hoàn thành Quiz
```
Quiz Service → POST /api/v1/gamify/challenge/event
{
  "eventType": "QUIZ",
  "action": "COMPLETE",
  "userId": "uuid",
  "entityId": "quiz-uuid",
  "score": 85,
  "occurredAt": "2025-01-15T10:30:00Z"
}
```

### 2. ChallengeProgressService xử lý
```
1. Lấy tất cả active challenges
2. Filter challenges có rule match với event
3. Với mỗi challenge match:
   - Lấy progress của user
   - Check daily limit
   - Check đã completed chưa
   - Increase progress
   - Check completion → trao thưởng nếu hoàn thành
```

### 3. Response
```json
{
  "code": 200,
  "result": {
    "processedChallenges": 2,
    "updatedProgress": [
      {
        "challengeId": "uuid",
        "currentProgress": 3,
        "targetProgress": 5,
        "completed": false
      }
    ],
    "completedChallenges": []
  },
  "message": "Event processed successfully"
}
```

---

## 📝 Database Schema Updates

### 1. Update challenges table
```sql
ALTER TABLE challenges
ADD COLUMN reward_score INT DEFAULT 0,
ADD COLUMN reward_badge VARCHAR(100),
ADD COLUMN reward_reason TEXT,
ADD COLUMN max_progress_per_day INT,
MODIFY COLUMN target INT NOT NULL;
```

### 2. Create user_challenge_progress table
```sql
CREATE TABLE user_challenge_progress (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    challenge_id BINARY(16) NOT NULL,
    current_progress INT NOT NULL DEFAULT 0,
    target_progress INT NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP NULL,
    last_progress_date DATE,
    progress_count_today INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_challenge (user_id, challenge_id),
    INDEX idx_user_id (user_id),
    INDEX idx_challenge_id (challenge_id),
    INDEX idx_completed (completed),
    FOREIGN KEY (challenge_id) REFERENCES challenges(challenge_id)
);
```

---

## 🚀 Implementation Roadmap

### Phase 1: Core Models & Database (Week 1)
1. Update Challenge model
2. Create UserChallengeProgress model
3. Create database migrations
4. Update repositories

### Phase 2: Rule System (Week 1-2)
1. Create ChallengeRule DTO
2. Create ChallengeRuleEvaluator
3. Create ChallengeEvent model
4. Implement rule parsing & evaluation

### Phase 3: Progress Service (Week 2)
1. Create ChallengeProgressService
2. Implement processEvent()
3. Implement daily limit checking
4. Implement completion logic

### Phase 4: APIs (Week 2-3)
1. POST /challenge/event endpoint
2. GET /challenge/{id}/progress endpoint
3. GET /challenge/me/active endpoint
4. Update existing endpoints

### Phase 5: Integration & Testing (Week 3)
1. Integrate với quiz-service
2. Unit tests
3. Integration tests
4. Documentation

---

## 📚 Notes

1. **Rule Format**: Hiện tại rule là JSON string, nên validate format khi save
2. **Daily Limit**: Cần scheduled job để reset `progressCountToday` mỗi ngày
3. **Performance**: Với nhiều challenges, nên cache active challenges trong Redis
4. **Event Processing**: Có thể dùng async processing để không block API response
5. **Error Handling**: Cần handle các trường hợp:
   - Invalid rule format
   - Event không match rule
   - Daily limit exceeded
   - Challenge đã completed

