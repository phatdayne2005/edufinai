# Learning Service API Documentation v2.0

## 🆕 What's New in v2.0
- **Level-based Access Control**: Learners can only enroll in lessons matching their level
- **Percentage-based EXP**: Progress shown as 0-100% instead of absolute points
- **Improvement-only Rewards**: EXP only increases when beating previous best score
- **Auto Level Up**: Learners automatically level up at 100% EXP

---

## Base URL
```
http://localhost:8080/learning
```

## Authentication
All endpoints (except public lesson listing) require JWT authentication.

**Header:**
```
Authorization: Bearer <jwt_token>
```

The JWT token is obtained from the Auth Service (`/identity/auth/token`).

---

## 1. Learner APIs

### 1.1 Get My Learner Profile ⚡ UPDATED
Retrieve the profile of the currently logged-in learner.

**Endpoint:** `GET /api/learners/me`

**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "level": "BEGINNER | INTERMEDIATE | ADVANCED",
  "totalExp": 15,
  "expPercent": 75
}
```

**Field Descriptions:**
- `level`: Current learner level (determines accessible lessons)
- `totalExp`: Total correct answers in current level
- `expPercent`: Progress percentage (0-100%) towards next level

**Example Progress:**
```
Level: BEGINNER
totalExp: 12 (answered 12 questions correctly)
expPercent: 75% (75% progress to INTERMEDIATE)

When expPercent reaches 100% → Level up to INTERMEDIATE → Reset to 0%
```

### 1.2 Get Learner by ID
**Endpoint:** `GET /api/learners/{id}`

**Response:** `200 OK` (Same as 1.1)

### 1.3 List All Learners
**Endpoint:** `GET /api/learners`

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "level": "BEGINNER",
    "totalExp": 15,
    "expPercent": 75
  }
]
```

### 1.4 List Learners by Level
**Endpoint:** `GET /api/learners/level/{level}`

**Path Parameters:**
- `level`: `BEGINNER` | `INTERMEDIATE` | `ADVANCED`

**Response:** `200 OK` (Array of LearnerRes)

---

## 2. Creator APIs

### 2.1 Get My Creator Profile
**Endpoint:** `GET /api/creators/me`

**Auth:** Required (Creator role)

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "username": "creator_username",
  "totalLessons": 0
}
```

### 2.2 Get Creator by ID
**Endpoint:** `GET /api/creators/{id}`

**Response:** `200 OK` (Same as 2.1)

### 2.3 List All Creators
**Endpoint:** `GET /api/creators`

**Response:** `200 OK` (Array of CreatorRes)

### 2.4 Get My Lessons
**Endpoint:** `GET /api/creators/me/lessons`

**Auth:** Required (`SCOPE_ROLE_CREATOR`)

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "title": "Lesson Title",
    "status": "DRAFT | PENDING | APPROVED | REJECTED",
    "difficulty": "BASIC | INTERMEDIATE | ADVANCED",
    "createdAt": "2024-01-01T10:00:00"
  }
]
```

---

## 3. Lesson APIs

### 3.1 List All Lessons
**Endpoint:** `GET /api/lessons`

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "title": "Introduction to Budgeting",
    "description": "Learn the basics of budgeting",
    "slug": "introduction-to-budgeting",
    "content": "Full lesson content...",
    "status": "APPROVED",
    "difficulty": "BASIC | INTERMEDIATE | ADVANCED",
    "durationMinutes": 30,
    "tags": ["BUDGETING", "SAVING"],
    "thumbnailUrl": "https://...",
    "videoUrl": "https://...",
    "quizJson": { ... },
    "totalQuestions": 10,
    "creatorId": "uuid",
    "moderatorId": "uuid",
    "createdAt": "2024-01-01T10:00:00Z",
    "updatedAt": "2024-01-01T10:00:00Z",
    "publishedAt": "2024-01-01T10:00:00Z"
  }
]
```

### 3.2 Get Lesson By ID
**Endpoint:** `GET /api/lessons/{id}`

**Auth:** Optional (Public)

**Response:** `200 OK` (LessonRes)

### 3.3 Get Lesson by Slug
**Endpoint:** `GET /api/lessons/slug/{slug}`

**Path Parameters:**
- `slug`: The unique slug of the lesson (e.g., `introduction-to-budgeting`)

**Response:** `200 OK` (LessonRes)

### 3.4 Filter Lessons by Tag
**Endpoint:** `GET /api/lessons/tags/{tag}`

**Path Parameters:**
- `tag`: `BUDGETING` | `INVESTING` | `SAVING` | `DEBT` | `TAX`

**Response:** `200 OK` (Array of LessonRes)

### 3.5 Filter Lessons by Difficulty
**Endpoint:** `GET /api/lessons/difficulty/{difficulty}`

**Path Parameters:**
- `difficulty`: `BASIC` | `INTERMEDIATE` | `ADVANCED`

**Response:** `200 OK` (Array of LessonRes)

### 3.6 Filter Lessons by Status
**Endpoint:** `GET /api/lessons/status/{status}`

**Path Parameters:**
- `status`: `DRAFT` | `PENDING` | `APPROVED` | `REJECTED`

**Response:** `200 OK` (Array of LessonRes)

### 3.7 Create Lesson
**Endpoint:** `POST /api/lessons`

**Auth:** Required (`SCOPE_ROLE_CREATOR`)

**Request Body:**
```json
{
  "title": "New Lesson",
  "description": "Description",
  "content": "<h1>Content</h1>",
  "durationMinutes": 30,
  "difficulty": "BASIC",
  "thumbnailUrl": "",
  "videoUrl": "",
  "tags": ["BUDGETING"],
  "quizJson": {
    "questions": [
      {
        "id": 1,
        "question": "Question?",
        "options": ["A", "B"],
        "correctAnswer": 0
      }
    ]
  }
}
```

**Response:** `200 OK` (LessonRes)

### 3.8 Update Lesson
**Endpoint:** `PUT /api/lessons/{lessonId}`

**Auth:** Required (`SCOPE_ROLE_CREATOR`)

**Request Body:** (Same as Create, all fields optional)

**Response:** `200 OK` (LessonRes)

### 3.9 Submit Lesson
**Endpoint:** `PUT /api/lessons/{lessonId}/submit`

**Auth:** Required (`SCOPE_ROLE_CREATOR`)

**Response:** `200 OK` (LessonRes with status = PENDING)

### 3.10 Update Lesson by Slug
**Endpoint:** `PUT /api/lessons/slug/{slug}`

**Auth:** Required (`SCOPE_ROLE_CREATOR`)

**Path Parameters:**
- `slug`: Lesson slug (e.g., `introduction-to-budgeting`)

**Request Body:** (Same as Update Le, all fields optional)

**Response:** `200 OK` (LessonRes)

**Note:** Alternative to update by ID, using SEO-friendly slug

### 3.11 Submit Lesson by Slug
**Endpoint:** `PUT /api/lessons/slug/{slug}/submit`

**Auth:** Required (`SCOPE_ROLE_CREATOR`)

**Path Parameters:**
- `slug`: Lesson slug

**Response:** `200 OK` (LessonRes with status = PENDING)

**Note:** Changes lesson status from DRAFT to PENDING for moderator review

### 3.12 Delete Lesson
**Endpoint:** `DELETE /api/lessons/{lessonId}`

**Auth:** Required (`SCOPE_ROLE_CREATOR`)

**Response:** `204 No Content`

### 3.13 Delete Lesson by Slug
**Endpoint:** `DELETE /api/lessons/slug/{slug}`

**Auth:** Required (`SCOPE_ROLE_CREATOR`)

**Path Parameters:**
- `slug`: Lesson slug

**Response:** `204 No Content`

---

## 4. Enrollment APIs

### 4.1 Enroll in Lesson ⚡ UPDATED
Create a new enrollment for a lesson.

**Endpoint:** `POST /api/enrollments`

**Auth:** Required (`SCOPE_ROLE_LEARNER`)

**Request Body:**
```json
{
  "lessonId": "uuid"
}
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "learnerId": "uuid",
  "lessonId": "uuid",
  "status": "IN_PROGRESS",
  "progressPercent": 0,
  "score": null,
  "attempts": 0,
  "correctAnswers": 0,
  "earnedExp": 0,
  "startedAt": "2024-01-01T10:00:00",
  "completedAt": null,
  "lastActivityAt": "2024-01-01T10:00:00"
}
```

**⚠️ Validation - Level Check:**
```json
// Error 400 - Level insufficient
{
  "code": 9999,
  "message": "Learner level BEGINNER is not sufficient to enroll in lesson with difficulty INTERMEDIATE. Required level: INTERMEDIATE"
}
```

**Access Rules:**
- `BEGINNER` → Can only enroll in `BASIC` lessons
- `INTERMEDIATE` → Can enroll in `BASIC` and `INTERMEDIATE` lessons
- `ADVANCED` → Can enroll in all lessons

### 4.2 Get My Enrollments
**Endpoint:** `GET /api/enrollments`

**Auth:** Required (`SCOPE_ROLE_LEARNER`)

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "learnerId": "uuid",
    "lessonId": "uuid",
    "status": "IN_PROGRESS",
    "progressPercent": 60,
    "score": 80,
    "attempts": 2,
    "correctAnswers": 3,
    "earnedExp": 3,
    "startedAt": "2024-01-01T10:00:00",
    "completedAt": null,
    "lastActivityAt": "2024-01-01T11:00:00"
  }
]
```

### 4.3 Get Enrollment Detail
**Endpoint:** `GET /api/enrollments/{enrollmentId}`

**Auth:** Required (`SCOPE_ROLE_LEARNER`)

**Response:** `200 OK` (EnrollmentRes)

**Error:** `403 Forbidden` if not the owner

### 4.4 Update Progress ⚡ UPDATED
Update enrollment progress and earn EXP.

**Endpoint:** `PUT /api/enrollments/{enrollmentId}/progress`

**Auth:** Required (`SCOPE_ROLE_LEARNER`)

**Request Body:**
```json
{
  "status": "IN_PROGRESS | COMPLETED",
  "progressPercent": 100,
  "score": 100,
  "addAttempt": 1,
  "correctAnswers": 5
}
```

**Field Descriptions:**
- `status`: Current status (auto-set to COMPLETED if all questions correct)
- `progressPercent`: Progress percentage (optional, auto-calculated)
- `score`: Quiz score (optional)
- `addAttempt`: Number of attempts to add (usually 1)
- `correctAnswers`: ⚡ **REQUIRED** - Number of correct answers in this attempt

**Response:** `200 OK` (GamificationRes)
```json
{
  "userId": "uuid",
  "sourceType": "QUIZ",
  "lessonId": "uuid",
  "enrollId": "uuid",
  "totalQuiz": 5,
  "correctAnswer": 5
}
```

**⚡ New EXP Logic:**

**Example - Lesson with 5 questions:**

| Attempt | Correct | Previous Best | EXP Gained | New Best | Status |
|---------|---------|---------------|------------|----------|--------|
| 1 | 3/5 | 0 | **+3** | 3 | IN_PROGRESS |
| 2 | 2/5 | 3 | **+0** | 3 | IN_PROGRESS (no improvement) |
| 3 | 4/5 | 3 | **+1** | 4 | IN_PROGRESS (improved by 1) |
| 4 | 5/5 | 4 | **+1** | 5 | ✅ COMPLETED |
| 5 | 5/5 | 5 | **+0** | 5 | COMPLETED (already maxed) |

**Rules:**
- ✅ Only gain EXP when improving best score
- ✅ Can retry unlimited times
- ✅ COMPLETED only when correctAnswers = totalQuestions
- ✅ Best score is preserved

### 4.5 Get My Enrollment (by Slug) 
**Endpoint:** `GET /api/enrollments/lessons/{slug}/my-enrollment`

**Auth:** Required (`SCOPE_ROLE_LEARNER`)

**Path Parameters:**
- `slug`: Lesson slug (e.g., `introduction-to-budgeting`)

**Response:** `200 OK` (EnrollmentRes)

**Error:** `404 Not Found` if not enrolled

### 4.6 Update Progress (by Slug)
**Endpoint:** `PUT /api/enrollments/lessons/{slug}/my-enrollment/progress`

**Auth:** Required (`SCOPE_ROLE_LEARNER`)

**Request Body:** (Same as 4.4)

**Response:** `200 OK` (GamificationRes)

---

## 5. Moderator APIs

### 5.1 List All Moderators
**Endpoint:** `GET /api/moderators`

**Auth:** Required (`SCOPE_ROLE_MOD`)

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "comment": "string",
    "pendingAssigned": 0
  }
]
```

### 5.2 List Lessons for Moderation
**Endpoint:** `GET /api/moderators/lessons`

**Auth:** Required (`SCOPE_ROLE_MOD`)

**Query Parameters:**
- `status` (optional): `PENDING` | `APPROVED` | `REJECTED` (default: `PENDING`)

**Response:** `200 OK` (Array of LessonRes)

**Note:** Moderators cannot view `DRAFT` lessons. Attempting to filter by `DRAFT` returns `403 Forbidden`.

### 5.3 View Lesson Detail
**Endpoint:** `GET /api/moderators/lessons/{lessonId}`

**Auth:** Required (`SCOPE_ROLE_MOD`)

**Response:** `200 OK` (LessonRes) or `403 Forbidden` if lesson is `DRAFT`

### 5.4 Moderate Lesson
**Endpoint:** `POST /api/moderators/lessons/{lessonId}/decision`

**Auth:** Required (`SCOPE_ROLE_MOD`)

**Request Body:**
```json
{
  "status": "APPROVED | REJECTED",
  "commentByMod": "Feedback for creator (max 2000 chars)"
}
```

**Response:** `200 OK` (LessonRes)

---

## 📊 EXP System Guide

### How EXP Works

**1. EXP is percentage-based (0-100%)**
- Each level has a threshold based on lesson difficulty
- Reaching 100% → Level up → Reset to 0%

**2. Calculation Formula:**
```
Threshold = 80% × Total questions in current difficulty
EXP% = (Total correct answers / Threshold) × 100%
```

**3. Example - BASIC difficulty:**
```
Total BASIC lessons: 4 lessons × 5 questions = 20 questions
Threshold: 20 × 80% = 16 questions
Current progress: 12 correct answers
EXP%: (12 / 16) × 100% = 75%
```

**4. Level Up:**
```
When EXP% = 100%:
- BEGINNER → INTERMEDIATE
- INTERMEDIATE → ADVANCED
- ADVANCED → Stay at ADVANCED
Reset totalExp and expPercent to 0
```

### Frontend Implementation

**Display Progress Bar:**
```jsx
// Use expPercent for progress bar
<ProgressBar 
  value={learner.expPercent} 
  max={100}
  label={`${learner.expPercent}% to ${nextLevel}`}
/>
```

**Handle Level Requirements:**
```jsx
// Disable enroll button if level insufficient
const canEnroll = (learnerLevel, lessonDifficulty) => {
  if (learnerLevel === 'BEGINNER') return lessonDifficulty === 'BASIC';
  if (learnerLevel === 'INTERMEDIATE') return ['BASIC', 'INTERMEDIATE'].includes(lessonDifficulty);
  return true; // ADVANCED can enroll in all
};

<Button 
  disabled={!canEnroll(learner.level, lesson.difficulty)}
  onClick={handleEnroll}
>
  {canEnroll(learner.level, lesson.difficulty) 
    ? 'Enroll' 
    : `Requires ${lesson.difficulty} level`}
</Button>
```

**Show Best Score:**
```jsx
// Display improvement opportunity
const enrollment = await getMyEnrollment(lessonSlug);
const bestScore = enrollment.correctAnswers;
const totalQuestions = lesson.totalQuestions;

<div>
  Best Score: {bestScore}/{totalQuestions}
  {bestScore < totalQuestions && (
    <p>Retry to improve and earn more EXP!</p>
  )}
</div>
```

---

## Error Responses

### 400 Bad Request
Invalid input data or validation errors.
```json
{
  "code": 9999,
  "message": "Learner level BEGINNER is not sufficient..."
}
```

### 401 Unauthorized
Missing or invalid JWT token.

### 403 Forbidden
User does not have permission.

### 404 Not Found
Resource not found.

### 500 Internal Server Error
Server error.

---

## Enums Reference

### LearnerLevel
- `BEGINNER` - Can access BASIC lessons
- `INTERMEDIATE` - Can access BASIC + INTERMEDIATE lessons  
- `ADVANCED` - Can access all lessons

### LessonDifficulty
- `BASIC`
- `INTERMEDIATE`
- `ADVANCED`

### LessonStatus
- `DRAFT` - Created but not submitted
- `PENDING` - Submitted for review
- `APPROVED` - Approved by moderator
- `REJECTED` - Rejected by moderator

### LessonTag
- `BUDGETING`
- `INVESTING`
- `SAVING`
- `DEBT`
- `TAX`

### EnrollmentStatus
- `IN_PROGRESS` - Currently learning
- `COMPLETED` - Finished with 100% correct answers

---

## Frontend Integration Checklist

### ✅ Required Updates for v2.0:

**1. Learner Profile Display:**
```jsx
// OLD
<div>EXP: {learner.totalExp} points</div>

// NEW
<div>
  <ProgressBar value={learner.expPercent} max={100} />
  <span>Level {learner.level} - {learner.expPercent}% to next level</span>
</div>
```

**2. Enroll Validation:**
```jsx
// Check before allowing enrollment
if (!canEnroll(learner.level, lesson.difficulty)) {
  showError(`Requires ${getRequiredLevel(lesson.difficulty)} level`);
  return;
}
```

**3. Progress Update:**
```jsx
// Include correctAnswers in update
await updateProgress(slug, {
  status: "COMPLETED",
  progressPercent: 100,
  score: score,
  addAttempt: 1,
  correctAnswers: correctCount // ⚡ REQUIRED
});
```

**4. Handle Level Up:**
```jsx
// Check if learner leveled up after update
const newProfile = await getMyProfile();
if (newProfile.level !== previousLevel) {
  showLevelUpAnimation(newProfile.level);
}
```

---

## Testing Guide

**Test Scenario 1: Level-based Access**
```
1. Login as BEGINNER learner
2. Try to enroll in INTERMEDIATE lesson
3. Expect: 400 error with message
4. Try to enroll in BASIC lesson
5. Expect: Success
```

**Test Scenario 2: EXP Improvement**
```
1. Enroll in 5-question lesson
2. Answer 3/5 correct → Check EXP increased
3. Answer 2/5 correct → Check EXP unchanged
4. Answer 5/5 correct → Check EXP increased & COMPLETED
5. Answer 5/5 again → Check EXP unchanged
```

**Test Scenario 3: Level Up**
```
1. Complete lessons until expPercent = 100%
2. Complete one more question
3. Check level increased
4. Check expPercent reset to 0%
5. Check can now access higher difficulty
```

---

## Support

For issues or questions:
1. Check `EXP_SYSTEM_DOCUMENTATION.md` for detailed explanations
2. Review `CHANGELOG.md` for recent changes
3. Use Postman collection for API testing
