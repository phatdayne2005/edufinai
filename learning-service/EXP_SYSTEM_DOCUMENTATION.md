# Learning Service - EXP System Documentation

## 📊 Hệ thống Kinh nghiệm (EXP) Mới

### Tổng quan

Hệ thống EXP đã được cập nhật từ **điểm số tuyệt đối** sang **phần trăm tiến độ (0-100%)** để thăng hạng.

---

## 🎯 Cơ chế Thăng hạng

### Quy tắc cơ bản:
- **EXP tính theo %**: Từ 0% → 100%
- **Đạt 100% EXP** → Thăng hạng → **Reset về 0%**
- **Thăng hạng = Mở khóa độ khó mới**

### Level Progression:
```
BEGINNER (Level 1)
    ↓ Đạt 100% EXP
INTERMEDIATE (Level 2)
    ↓ Đạt 100% EXP
ADVANCED (Level 3) - Max level
```

---

## 📐 Công thức tính EXP%

### Công thức:
```
EXP% = (Tổng số câu đúng / Ngưỡng thăng hạng) × 100%

Ngưỡng thăng hạng = 80% × Tổng số câu hỏi trong độ khó hiện tại
```

### Ví dụ cụ thể:

**Độ khó BASIC:**
- Tổng số bài: 4 bài (APPROVED)
- Mỗi bài: 5 câu hỏi
- **Tổng số câu**: 4 × 5 = 20 câu
- **Ngưỡng thăng hạng**: 20 × 80% = 16 câu đúng

**Tính EXP%:**
- User đúng 8 câu → EXP = (8/16) × 100% = **50%**
- User đúng 16 câu → EXP = (16/16) × 100% = **100%** → **THĂNG HẠNG!**

---

## 🎮 Cơ chế cộng điểm

### Nguyên tắc: **Chỉ cộng khi cải thiện**

#### Quy tắc:
1. **Best Score**: Lưu số câu đúng tối đa đã đạt được trong mỗi bài
2. **Cộng Delta**: Chỉ cộng phần chênh lệch khi cải thiện
3. **Không giới hạn lần làm**: Làm lại nhiều lần để đạt 100%
4. **COMPLETED**: Chỉ khi đúng HẾT câu hỏi (100%)

#### Ví dụ chi tiết:

**Bài học có 5 câu hỏi:**

| Lần | Đúng | Best trước | Cộng EXP | Best sau | Status | Lý do |
|-----|------|------------|----------|----------|---------|-------|
| 1 | 3/5 | 0 | **+3** | 3 | IN_PROGRESS | Cải thiện từ 0 → 3 |
| 2 | 2/5 | 3 | **+0** | 3 | IN_PROGRESS | Không cải thiện (2 < 3) |
| 3 | 4/5 | 3 | **+1** | 4 | IN_PROGRESS | Cải thiện từ 3 → 4 |
| 4 | 5/5 | 4 | **+1** | 5 | ✅ **COMPLETED** | Cải thiện từ 4 → 5 + Đúng 100% |
| 5 | 5/5 | 5 | **+0** | 5 | COMPLETED | Đã max rồi |
| 6 | 3/5 | 5 | **+0** | 5 | COMPLETED | Best vẫn là 5 |

**Lợi ích:**
- Khuyến khích làm lại để đạt 100%
- Không mất điểm khi làm kém hơn
- Công bằng: chỉ thưởng khi tiến bộ

---

## 🔒 Giới hạn Level

### Quy tắc enroll:
- **BEGINNER**: Chỉ học bài BASIC
- **INTERMEDIATE**: Học bài BASIC + INTERMEDIATE
- **ADVANCED**: Học tất cả bài

### Backend Validation:
```java
@PostMapping("/enrollments")
// ✅ Kiểm tra learner.level >= lesson.difficulty
// ❌ Throw exception nếu level không đủ
```

### Frontend:
- Disable nút "Enroll" nếu level chưa đủ
- Vẫn cho **xem nội dung** (preview)
- Hiển thị thông báo level yêu cầu

---

## 🗄️ Database Changes

### Thêm column mới:
```sql
ALTER TABLE learner 
ADD COLUMN exp_percent INT DEFAULT 0;
```

### Ý nghĩa các field:
- `level`: Cấp độ hiện tại (BEGINNER/INTERMEDIATE/ADVANCED)
- `total_exp`: Tổng số câu đúng đã đạt (trong cùng level)
- `exp_percent`: Phần trăm tiến độ thăng hạng (0-100%)

### Enrollment fields:
- `correct_answers`: Số câu đúng tối đa đã đạt (best score)
- `earned_exp`: = `correct_answers` (tracking)
- `status`: COMPLETED chỉ khi đúng 100%

---

## 🔧 API Changes

### LearnerRes:
```json
{
  "id": "uuid",
  "level": "INTERMEDIATE",
  "totalExp": 25,
  "expPercent": 78  // ← MỚI: 0-100%
}
```

### EnrollmentProgressReq:
```json
{
  "status": "COMPLETED",
  "progressPercent": 100,
  "score": 80,
  "addAttempt": 1,
  "correctAnswers": 5  // ← BẮT BUỘC
}
```

---

## 📝 Migration Guide

### Bước 1: Chạy migration SQL
```bash
mysql -u root -p learning < migration_exp_percent.sql
```

### Bước 2: Update code
- Pull latest code
- Build lại project

### Bước 3: Testing
```bash
# Test enroll với level không đủ → Expect 400/403
# Test làm bài nhiều lần → Check best score
# Test thăng hạng → Check EXP% = 0 after level up
```

---

## ✅ Checklist

- [x] Model: Thêm `expPercent` vào Learner
- [x] DTO: Thêm `expPercent` vào LearnerRes
- [x] Repository: Query `getTotalQuestionsByDifficulty()`
- [x] Service: Logic EXP% và level up
- [x] Service: Logic cộng điểm theo improvement
- [x] Validation: Check level khi enroll
- [x] Migration: SQL script
- [x] Documentation: File này

---

## 🎯 Testing Scenarios

### Test 1: Cộng điểm improvement
```
1. Enroll vào bài 5 câu
2. Làm bài đúng 3/5 → API trả về +3 EXP
3. Làm bài đúng 2/5 → API trả về +0 EXP
4. Làm bài đúng 5/5 → API trả về +2 EXP (delta)
5. Status = COMPLETED
```

### Test 2: Thăng hạng
```
1. User BEGINNER, BASIC có 20 câu total
2. Ngưỡng = 16 câu (80%)
3. Làm bài đến khi tổng đúng = 16 câu
4. Sau làm bài: Level = INTERMEDIATE, EXP% = 0%
```

### Test 3: Validation
```
1. User BEGINNER enroll INTERMEDIATE lesson
2. Expect: 400 Bad Request
3. Message: "Learner level BEGINNER is not sufficient..."
```

---

## 📞 Support

Nếu có vấn đề, kiểm tra:
1. Database có column `exp_percent` chưa?
2. `spring.jpa.hibernate.ddl-auto` = update?
3. Code đã build lại chưa?
4. Frontend đã update API call chưa?
