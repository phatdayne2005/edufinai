package vn.uth.learningservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.learningservice.model.Enrollment;
import vn.uth.learningservice.repository.EnrollmentRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepo;
    private final LearnerService learnerService;

    public Enrollment getById(UUID id) {
        return enrollmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + id));
    }

    public List<Enrollment> listByLearner(UUID learnerId) {
        return enrollmentRepo.findByLearner_Id(learnerId);
    }

    @Transactional
    public Enrollment enrollIfAbsent(Enrollment newEnroll) {
        UUID learnerId = newEnroll.getLearner().getId();
        UUID lessonId = newEnroll.getLesson().getId();
        if (enrollmentRepo.existsByLearner_IdAndLesson_Id(learnerId, lessonId)) {
            return enrollmentRepo.findByLearner_IdAndLesson_Id(learnerId, lessonId).get();
        }
        return enrollmentRepo.save(newEnroll);
    }

    @Transactional
    public void updateProgress(UUID enrollmentId,
            Enrollment.Status status,
            int progressPercent,
            Integer score,
            int addAttempt) {
        if (progressPercent < 0 || progressPercent > 100) {
            throw new IllegalArgumentException("progressPercent must be 0..100");
        }

        // Lấy enrollment trước khi update để kiểm tra status cũ và lấy learnerId
        Enrollment enrollmentBefore = enrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));

        Enrollment.Status oldStatus = enrollmentBefore.getStatus();
        UUID learnerId = enrollmentBefore.getLearner().getId();

        // Update enrollment
        int updated = enrollmentRepo.updateProgress(enrollmentId, status, progressPercent, score, addAttempt);
        if (updated == 0)
            throw new IllegalArgumentException("Enrollment not found: " + enrollmentId);

        // Tự động cộng điểm cho learner khi lesson được hoàn thành
        // Chỉ cộng điểm khi:
        // 1. Status chuyển sang COMPLETED (tránh cộng nhiều lần)
        // 2. Progress đạt 100% (đã làm hết câu hỏi)
        // 3. Có score (score != null và > 0)
        if (status == Enrollment.Status.COMPLETED
                && oldStatus != Enrollment.Status.COMPLETED
                && progressPercent == 100
                && score != null
                && score > 0) {
            // Tính điểm sẽ cộng: score chính là điểm học tập (Số câu đúng * 10 do FE tính
            // và gửi lên)
            int pointsToAdd = score;
            learnerService.addPoints(learnerId, pointsToAdd);
        }
    }

    public long countCompletedByLearner(UUID learnerId) {
        return enrollmentRepo.countCompletedByLearner(learnerId);
    }
}
