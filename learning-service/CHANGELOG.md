# Learning Service - Changelog

## Version 2.0 - EXP System Overhaul (2025-11-25)

### 🎯 Tổng quan thay đổi

Đã hoàn thành 2 yêu cầu chính:
1. **Backend validation**: Kiểm tra level khi enroll
2. **Hệ thống EXP mới**: Từ điểm số tuyệt đối → phần trăm tiến độ

---

## 📋 Danh sách file đã thay đổi

### ✅ Models
- `Learner.java`
  - Thêm field `expPercent` (Integer, 0-100%)
  - Update comments cho `totalExp`

### ✅ DTOs
- `LearnerRes.java`
  - Thêm field `expPercent`

### ✅ Controllers
- `EnrollmentController.java`
  - Thêm validation `canEnrollInLesson()` - check level vs difficulty
  - Thêm helper method `getRequiredLevelForDifficulty()`
  - Throw exception khi level không đủ

### ✅ Services
- `LearnerService.java` - **Thay đổi lớn**
  - ✨ Method mới: `addCorrectAnswers()` - cộng số câu đúng
  - ✨ Method mới: `updateExpPercentAndLevel()` - tính EXP% và level up
  - ✨ Method mới: `getDifficultyForLevel()` - helper
  - 🔧 Inject `LessonRepository` để query total questions
  - ⚠️ Deprecated: `addExp()` - backward compatibility

- `EnrollmentService.java` - **Thay đổi lớn**
  - Logic mới: Chỉ cộng EXP khi `current > best`
  - Track `previousBestScore` trong enrollment
  - COMPLETED chỉ khi đúng 100% câu
  - Progress% based on best score
  - Call `addCorrectAnswers()` thay vì `addExp()`

### ✅ Repositories
- `LessonRepository.java`
  - Thêm query: `getTotalQuestionsByDifficulty()`
  - Tính tổng số câu của tất cả bài APPROVED trong 1 difficulty

### ✅ Documentation
- `EXP_SYSTEM_DOCUMENTATION.md` - File mới
  - Giải thích đầy đủ cơ chế mới
  - Công thức tính EXP%
  - Ví dụ cụ thể
  - Migration guide
  - Testing scenarios

### ✅ Migration
- `migration_exp_percent.sql` - File mới
  - ALTER TABLE để thêm column `exp_percent`
  - Add constraint (0-100)
  - Set default values

### ✅ Postman Collection
- `learning-service-postman.json`
  - Đã cập nhật body với `correctAnswers` field
  - Đã thêm section Gamification

---

## 🔧 Database Changes

### Thêm column:
```sql
ALTER TABLE learner 
ADD COLUMN exp_percent INT DEFAULT 0 
COMMENT 'Experience percentage (0-100%) towards next level';

ALTER TABLE learner 
ADD CONSTRAINT chk_exp_percent 
CHECK (exp_percent >= 0 AND exp_percent <= 100);
```

### Ý nghĩa field changes:
| Field | Trước | Sau |
|-------|-------|-----|
| `total_exp` | Tổng EXP points | Tổng số câu đúng (trong current level) |
| `exp_percent` | ❌ Không có | Phần trăm tiến độ (0-100%) |

---

## 📊 Logic Changes Summary

### 1. Enrollment Validation (YÊU CẦU 1)

**Trước:**
```java
// Không có validation
Enrollment newEnroll = new Enrollment();
enrollment Repo.save(newEnroll);
```

**Sau:**
```java
// ✅ Check level vs difficulty
if (!canEnrollInLesson(learner.getLevel(), lesson.getDifficulty())) {
    throw new IllegalArgumentException("Level not sufficient");
}
```

**Rules:**
- BEGINNER → BASIC only
- INTERMEDIATE → BASIC + INTERMEDIATE
- ADVANCED → All difficulties

---

### 2. EXP Calculation (YÊU CẦU 2)

#### A. Cộng điểm

**Trước:**
```java
// Mỗi lần làm bài đều cộng
long exp = correctAnswers * 10L;
learner.setTotalExp(totalExp + exp);
```

**Sau:**
```java
// ✅ Chỉ cộng khi cải thiện
int delta = currentCorrect - previousBest;
if (delta > 0) {
    learnerService.addCorrectAnswers(learnerId, delta);
}
```

#### B. Thăng hạng

**Trước:**
```java
// Threshold cố định
if (exp >= 1000) level = INTERMEDIATE;
if (exp >= 5000) level = ADVANCED;
```

**Sau:**
```java
// ✅ Dựa trên tổng số câu của difficulty
int threshold = totalQuestions * 80 / 100;
double expPercent = (totalCorrect / threshold) * 100;
if (expPercent >= 100) {
    levelUp();
    reset();
}
```

#### C. Status COMPLETED

**Trước:**
```java
// Set COMPLETED khi đạt threshold nào đó
if (correctAnswers >= totalQuestions) {
    status = COMPLETED;
}
```

**Sau:**
```java
// ✅ Chỉ khi đúng HẾT câu
if (currentCorrect >= totalQuestions) {
    status = COMPLETED;
}
// Best score vẫn được giữ nguyên
```

---

## ⚠️ Breaking Changes

### API Response Changes:
```json
// LearnerRes - BEFORE
{
  "id": "uuid",
  "level": "BEGINNER",
  "totalExp": 150
}

// LearnerRes - AFTER
{
  "id": "uuid",
  "level": "BEGINNER",
  "totalExp": 15,        // ← Ý nghĩa đổi: số câu đúng
  "expPercent": 75       // ← MỚI: phần trăm
}
```

### API Behavior Changes:

1. **Enroll endpoint**:
   - ❌ CŨ: Luôn cho enroll
   - ✅ MỚI: Throw exception nếu level không đủ

2. **Update Progress**:
   - ❌ CŨ: Mỗi lần làm đều cộng EXP
   - ✅ MỚI: Chỉ cộng khi improvement

---

## 🧪 Testing Checklist

### Backend Tests Needed:

- [ ] Test enroll validation
  - BEGINNER enroll INTERMEDIATE → Expect error
  - INTERMEDIATE enroll BASIC → Expect success

- [ ] Test EXP calculation
  - Làm bài lần 1: 3/5 → +3 EXP
  - Làm bài lần 2: 2/5 → +0 EXP
  - Làm bài lần 3: 5/5 → +2 EXP

- [ ] Test level up
  - Đạt 80% câu đúng → Level up → EXP reset

- [ ] Test COMPLETED status
  - 4/5 câu → IN_PROGRESS
  - 5/5 câu → COMPLETED
  - Làm lại → Vẫn COMPLETED

### Frontend Tests Needed:

- [ ] Display EXP progress bar (0-100%)
- [ ] Disable enroll button khi level không đủ
- [ ] Show level up animation
- [ ] Update learner profile with new fields

---

## 📝 Migration Steps

### For Developers:

1. **Pull latest code**
   ```bash
   git pull origin main
   ```

2. **Run migration (nếu cần)**
   ```bash
   mysql -u root -p learning < migration_exp_percent.sql
   ```
   
3. **Build project**
   ```bash
   mvn clean install
   ```

4. **Start service**
   ```bash
   mvn spring-boot:run
   ```

### For Production:

1. Backup database
2. Run migration script
3. Deploy new code
4. Monitor logs for errors
5. Test critical flows

---

## 🐛 Known Issues

None at the moment.

---

## 📞 Support

Nếu gặp vấn đề:
1. Check logs: `logs/learning-service.log`
2. Verify database: `SELECT * FROM learner LIMIT 5;`
3. Test API via Postman collection
4. Contact: [Your contact info]

---

## 👥 Contributors

- Backend Implementation: AI Assistant
- Requirements: User
- Testing: Pending

---

## 📅 Timeline

- **2025-11-25**: Requirements finalized
- **2025-11-25**: Implementation completed
- **TBD**: Testing & deployment
