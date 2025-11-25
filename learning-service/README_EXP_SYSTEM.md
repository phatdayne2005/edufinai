# 🎓 Learning Service - Quick Start

## 🆕 Latest Updates (v2.0)

### ✨ New Features:
1. **Level-based Access Control**: Users can only enroll in lessons matching their level
2. **Percentage-based EXP System**: Progress shown as 0-100% instead of absolute points
3. **Improvement-only Rewards**: Only gain EXP when improving your best score
4. **Smart Level Up**: Automatically level up when reaching 100% EXP

---

## 🚀 Quick API Examples

### Get My Learner Profile
```bash
GET /api/learners/me
Authorization: Bearer {token}

Response:
{
  "id": "uuid",
  "level": "INTERMEDIATE",
  "totalExp": 25,
  "expPercent": 78  // 78% progress to next level
}
```

### Enroll in Lesson (with validation)
```bash
POST /api/enrollments
Authorization: Bearer {token}
Content-Type: application/json

{
  "lessonId": "lesson-uuid"
}

# ✅ Success if learner.level >= lesson.difficulty
# ❌ Error 400 if level insufficient
```

### Update Progress (improvement-based)
```bash
PUT /api/enrollments/{id}/progress
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": "COMPLETED",
  "progressPercent": 100,
  "score": 100,
  "addAttempt": 1,
  "correctAnswers": 5  // Required!
}

# Only adds EXP if correctAnswers > previous best
```

---

## 📊 EXP System Overview

### How it works:
1. Each level has a difficulty (BEGINNER→BASIC, INTERMEDIATE→INTERMEDIATE, ADVANCED→ADVANCED)
2. EXP% = (Your correct answers / 80% of total questions in difficulty) × 100%
3. Reach 100% → Level up → Reset to 0%
4. Higher level = Access to harder lessons

### Example:
```
BASIC difficulty: 20 total questions
Level up threshold: 20 × 80% = 16 questions

Your progress:
- 8 correct answers  → 50% EXP
- 12 correct answers → 75% EXP
- 16 correct answers → 100% EXP → LEVEL UP! 🎉
```

---

## 📁 Important Files

- **`EXP_SYSTEM_DOCUMENTATION.md`**: Detailed system explanation
- **`CHANGELOG.md`**: All changes and migration guide
- **`migration_exp_percent.sql`**: Database migration script
- **`learning-service-postman.json`**: API testing collection

---

## 🔧 Setup

### 1. Database Migration
```bash
# Option A: Auto (Hibernate will create expPercent column)
spring.jpa.hibernate.ddl-auto=update

# Option B: Manual
mysql -u root -p learning < migration_exp_percent.sql
```

### 2. Run Service
```bash
mvn clean install
mvn spring-boot:run
```

### 3. Test with Postman
- Import `learning-service-postman.json`
- Get token from Auth endpoint
- Test enroll with different levels

---

## ⚠️ Important Notes

### For Frontend Developers:
- LearnerRes now has `expPercent` field (0-100%)
- Display as progress bar for better UX
- Enroll API may return 400 if level insufficient
- Handle error and show "Level X required" message

### For Backend Developers:
- Use `addCorrectAnswers()` instead of deprecated `addExp()`
- Enrollment validation happens in controller
- EXP calculation moved to LearnerService

---

## 📞 Need Help?

1. Read `EXP_SYSTEM_DOCUMENTATION.md` for details
2. Check `CHANGELOG.md` for what changed
3. Review Postman collection for API examples
4. Check logs if something doesn't work

---

## 🎯 Key Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/learners/me` | GET | Get my profile (with expPercent) |
| `/learners/{id}` | GET | Get learner by ID |
| `/enrollments` | POST | Enroll in lesson (validated) |
| `/enrollments/{id}/progress` | PUT | Update progress (improvement-based) |
| `/lessons/{slug}/my-enrollment` | GET | Get my enrollment by slug |
| `/learning/gamify-response` | GET | Get gamification data |

---

Happy coding! 🚀
