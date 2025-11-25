# Learning Service API Documentation

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

### 1.1 Get My Learner Profile
Retrieve the profile of the currently logged-in learner.

**Endpoint:** `GET /api/learners/me`

**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "level": "BEGINNER | INTERMEDIATE | ADVANCED",
  "totalPointsLearning": 0
}
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
    "totalPointsLearning": 0
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

### 2.4 Get My Lessons ✨ NEW
**Endpoint:** `GET /api/creators/me/lessons`

**Auth:** Required (`SCOPE_ROLE_CREATOR`)

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "title": "Lesson Title",
    "status": "DRAFT | PENDING | APPROVED | REJECTED",
    "createdAt": "2024-01-01T10:00:00"
    // ... other lesson fields
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
    "status": "DRAFT | PENDING | APPROVED | REJECTED",
    "difficulty": "BASIC | INTERMEDIATE | ADVANCED",
    "durationMinutes": 30,
    "tags": ["BUDGETING", "SAVING"],
    "thumbnailUrl": "https://...",
    "videoUrl": "https://...",
    "commentByMod": "Optional feedback from moderator",
    "quizJson": { ... },
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

### 3.3 Get Lesson by Slug ✨ NEW
**Endpoint:** `GET /api/lessons/slug/{slug}`

**Path Parameters:**
- `slug`: The unique slug of the lesson (e.g., `introduction-to-budgeting`)

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "title": "Introduction to Budgeting",
  "description": "Learn the basics of budgeting",
  "slug": "introduction-to-budgeting",
  "content": "Full lesson content...",
  "status": "APPROVED",
  "difficulty": "BASIC",
  "durationMinutes": 30,
  "tags": ["BUDGETING", "SAVING"],
  "thumbnailUrl": "https://...",
  "videoUrl": "https://...",
  "commentByMod": null,
  "quizJson": {
    "questions": [
      {
        "id": 1,
        "question": "What is budgeting?",
        "options": ["A", "B", "C", "D"],
        "correctAnswer": 0
      }
    ]
  },
  "creatorId": "uuid",
  "moderatorId": "uuid",
  "createdAt": "2024-01-01T10:00:00Z",
  "updatedAt": "2024-01-01T10:00:00Z",
  "publishedAt": "2024-01-01T10:00:00Z"
}
```

**Error:** `404 Not Found` if slug doesn't exist
```json
{
  "code": 9999,
  "message": "Lesson not found with slug: introduction-to-budgeting"
}
```

**Note:** This endpoint is useful for creating SEO-friendly URLs (e.g., `/learning/lesson/introduction-to-budgeting` instead of `/learning/lesson/550e8400-...`). The slug is automatically generated when creating a lesson.

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

  "learnerId": "uuid",
  "lessonId": "uuid",
  "status": "IN_PROGRESS",
  "progressPercent": 50,
  "score": null,
  "attempts": 1,
  "startedAt": "2024-01-01T10:00:00",
  "completedAt": null,
  "lastActivityAt": "2024-01-01T11:00:00",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T11:00:00"
}
```

**Error Responses:**
- `404 Not Found` - Lesson with this slug doesn't exist
- `404 Not Found` - User is not enrolled in this lesson (message: "Not enrolled in this lesson")

**Note:** This endpoint uses lesson slug + JWT token to identify the enrollment, eliminating the need to pass enrollment ID in URLs. Perfect for SEO-friendly routes like `/learning/quiz/introduction-to-budgeting`.

### 4.6 Update My Enrollment Progress (by Slug) ✨ NEW
**Endpoint:** `PUT /api/enrollments/lessons/{slug}/my-enrollment/progress`

**Auth:** Required (`SCOPE_ROLE_LEARNER`)

**Path Parameters:**
- `slug`: The lesson slug (e.g., `introduction-to-budgeting`)

**Request Body:**
```json
{
  "status": "IN_PROGRESS | COMPLETED | DROPPED (required)",
  "progressPercent": 100,
  "score": 80,
  "addAttempt": 1
}
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "learnerId": "uuid",
  "lessonId": "uuid",
  "status": "COMPLETED",
  "progressPercent": 100,
  "score": 80,
  "attempts": 2,
  "startedAt": "2024-01-01T10:00:00",
  "completedAt": "2024-01-01T12:00:00",
  "lastActivityAt": "2024-01-01T12:00:00",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T12:00:00"
}
```

**Error Responses:**
- `404 Not Found` - Lesson with this slug doesn't exist
- `404 Not Found` - User is not enrolled in this lesson

**Note:** Frontend can now update progress without knowing the enrollment ID. Just pass the lesson slug and the system will automatically find the correct enrollment for the authenticated user.

**Example Usage in Frontend:**
```javascript
// Old way (enrollment ID in URL):
PUT /api/enrollments/550e8400.../progress

// New way (clean URL):
PUT /api/enrollments/lessons/introduction-to-budgeting/my-enrollment/progress
```

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

### 5.4 Moderate Lesson (Approve/Reject)
**Endpoint:** `POST /api/moderators/lessons/{lessonId}/decision`

**Auth:** Required (`SCOPE_ROLE_MOD`)

**Request Body:**
```json
{
  "status": "APPROVED | REJECTED (required)",
  "commentByMod": "string (max 2000 chars, feedback for creator)"
}
```

**Response:** `200 OK` (LessonRes)

---

## Error Responses

### 400 Bad Request
Invalid input data (validation errors).

### 401 Unauthorized
Missing or invalid JWT token.

### 403 Forbidden
User does not have permission to access the resource.

### 404 Not Found
Resource (lesson, enrollment, etc.) not found.

### 500 Internal Server Error
Server error.

**Error Response Format:**
```json
{
  "code": 9999,
  "message": "Error description"
}
```

---

## Enums Reference

### LearnerLevel
- `BEGINNER`
- `INTERMEDIATE`
- `ADVANCED`

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
- `IN_PROGRESS` - Learner is currently taking the lesson
- `COMPLETED` - Lesson completed
- `DROPPED` - Learner stopped taking the lesson

---

## Notes for Frontend Integration

1. **Authentication Flow:**
   - Login via Auth Service: `POST /identity/auth/token`
   - Store JWT token
   - Include token in all subsequent requests

2. **User Roles:**
   - `SCOPE_ROLE_LEARNER` - Can enroll in lessons and track progress
   - `SCOPE_ROLE_CREATOR` - Can create and manage lessons
   - `SCOPE_ROLE_MODERATOR` - Can approve/reject lessons

3. **Lesson Workflow:**
   - Creator: Create → Submit → (Wait for approval)
   - Moderator: Review → Approve/Reject
   - Learner: Enroll → Learn → Complete

4. **Learning Progress:**
   - Learners earn points by completing lessons
   - Points are calculated based on quiz score
   - Progress is tracked per enrollment

5. **Quiz Structure:**
   The `quizJson` field should follow this structure:
   ```json
   {
     "questions": [
       {
         "id": 1,
         "question": "Question text?",
         "options": ["Option A", "Option B", "Option C", "Option D"],
         "correctAnswer": 0
       }
     ]
   }
   ```

6. **SEO-Friendly URLs with Context-Aware Endpoints:** ✨ NEW
   
   **Problem:** UUID-based URLs are ugly and not SEO-friendly:
   ```
   ❌ /learning/lesson/550e8400-e29b-41d4-a716-446655440000
   ❌ /learning/quiz/550e8400-e29b-41d4-a716-446655440000
   ❌ /enrollment/e7f4b8a2-3c1d-4f9e-b6a5-1234567890ab/progress
   ```

   **Solution:** Use lesson slugs + context-aware endpoints:
   ```
   ✅ /learning/lesson/introduction-to-budgeting
   ✅ /learning/quiz/introduction-to-budgeting
   ✅ Update progress via: PUT /api/enrollments/lessons/introduction-to-budgeting/my-enrollment/progress
   ```

   **How it works:**
   - **Lesson Slug:** Automatically generated from lesson title when created
   - **Context-Aware:** Backend uses JWT token (user ID) + slug (lesson) to find the correct resource
   - **No ID in URL:** Enrollment ID is never exposed in URLs

   **Frontend Implementation Example:**
   ```javascript
   // In QuizPage or LessonDetailPage component
   const { slug } = useParams(); // Get slug from URL
   
   // Fetch lesson by slug
   const lesson = await api.getLessonBySlug(slug);
   
   // Get my enrollment for this lesson (no need to know enrollment ID)
   const enrollment = await api.getMyEnrollmentForLesson(slug);
   
   // Submit quiz results
   await api.updateMyEnrollmentProgress(slug, {
     status: "COMPLETED",
     progressPercent: 100,
     score: 80,
     addAttempt: 1
   });
   ```

   **Benefits:**
   - Clean, readable URLs
   - Better SEO
   - Easier to share
   - Less chance of accidentally exposing sensitive IDs
   - Simpler frontend code


---

## 6. AI Integration APIs

### 6.1 Get Lessons for AI
**Endpoint:** `GET /api/learning/ai-response`

**Auth:** Required (Any role)

**Response:** `200 OK`
```json
[
  {
    "lessonId": "uuid",
    "title": "Lesson Title",
    "description": "Lesson Description",
    "slug": "lesson-slug"
  }
]
```
