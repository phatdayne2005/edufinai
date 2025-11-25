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

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public Enrollment getById(UUID id) {
        return enrollmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + id));
    }

    // ... (keep existing methods)

    public List<Enrollment> listByLearner(UUID learnerId) {
        return enrollmentRepo.findByLearner_Id(learnerId);
    }

    public Optional<Enrollment> findByLearnerAndLesson(UUID learnerId, UUID lessonId) {
        return enrollmentRepo.findByLearner_IdAndLesson_Id(learnerId, lessonId);
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
    public vn.uth.learningservice.dto.response.GamificationRes updateProgress(UUID enrollmentId,
            vn.uth.learningservice.dto.request.EnrollmentProgressReq req) {
        Enrollment enrollment = enrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));

        // 1. Calculate Total Questions from Lesson Quiz JSON
        int totalQuestions = 0;
        try {
            String quizJsonStr = enrollment.getLesson().getQuizJson();
            if (quizJsonStr != null && !quizJsonStr.isBlank()) {
                com.fasterxml.jackson.databind.JsonNode quiz = objectMapper.readTree(quizJsonStr);
                if (quiz != null && quiz.has("questions")) {
                    totalQuestions = quiz.get("questions").size();
                }
            }
        } catch (Exception e) {
            // log error or ignore
        }
        enrollment.setTotalQuizQuestions(totalQuestions);

        // 2. Get Correct Answers Count
        int correctAnswers = req.getCorrectAnswersCount() != null ? req.getCorrectAnswersCount() : 0;
        enrollment.setCorrectAnswersCount(correctAnswers);

        // 3. Determine Status & Progress
        // Only COMPLETED if correct all questions (and total > 0)
        if (totalQuestions > 0 && correctAnswers >= totalQuestions) {
            enrollment.setStatus(Enrollment.Status.COMPLETED);
            enrollment.setCompletedAt(java.time.LocalDateTime.now());
            enrollment.setProgressPercent(100);
        } else {
            // If not completed, ensure status is IN_PROGRESS (unless previously completed?
            // User requirement: "Only change to COMPLETED if correct all".
            // Implies if not correct all, it's not COMPLETED.
            // But if user retries a completed lesson and gets fewer points, should we
            // revert status?
            // Usually no. Once completed, always completed.
            if (enrollment.getStatus() != Enrollment.Status.COMPLETED) {
                enrollment.setStatus(Enrollment.Status.IN_PROGRESS);
                if (totalQuestions > 0) {
                    enrollment.setProgressPercent((correctAnswers * 100) / totalQuestions);
                }
            }
        }

        // Update other fields
        enrollment.setAttempts(enrollment.getAttempts() + req.getAddAttempt());
        enrollment.setLastActivityAt(java.time.LocalDateTime.now());
        if (req.getScore() != null)
            enrollment.setScore(req.getScore());

        // 4. Calculate Exp
        // Rule: 1 correct = 10 exp. Max exp = totalQuestions * 10.
        // Accumulate exp up to max.
        long maxExp = totalQuestions * 10L;
        long currentAttemptExp = correctAnswers * 10L;

        long previousEarned = enrollment.getEarnedExp() != null ? enrollment.getEarnedExp() : 0L;
        long expToAdd = 0;

        if (currentAttemptExp > previousEarned) {
            expToAdd = currentAttemptExp - previousEarned;
            // Cap at maxExp
            if (previousEarned + expToAdd > maxExp) {
                expToAdd = maxExp - previousEarned;
            }

            if (expToAdd > 0) {
                enrollment.setEarnedExp(previousEarned + expToAdd);
                learnerService.addExp(enrollment.getLearner().getId(), expToAdd);
            }
        }

        enrollmentRepo.save(enrollment);

        // 5. Return Gamification Data
        return vn.uth.learningservice.dto.response.GamificationRes.builder()
                .userId(enrollment.getLearner().getId())
                .sourceType("QUIZ")
                .lessonId(enrollment.getLesson().getId())
                .enrollId(enrollment.getId())
                .totalQuiz(totalQuestions)
                .correctAnswer(correctAnswers)
                .build();
    }

    public long countCompletedByLearner(UUID learnerId) {
        return enrollmentRepo.countCompletedByLearner(learnerId);
    }
}
