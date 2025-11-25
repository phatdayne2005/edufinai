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

        // 1. Get total questions from lesson
        int totalQuestions = enrollment.getLesson().getTotalQuestions() != null
                ? enrollment.getLesson().getTotalQuestions()
                : 0;

        // Fallback if totalQuestions is 0 (for old lessons)
        if (totalQuestions == 0) {
            try {
                String quizJsonStr = enrollment.getLesson().getQuizJson();
                if (quizJsonStr != null && !quizJsonStr.isBlank()) {
                    com.fasterxml.jackson.databind.JsonNode quiz = objectMapper.readTree(quizJsonStr);
                    if (quiz != null) {
                        if (quiz.has("questions")) {
                            totalQuestions = quiz.get("questions").size();
                        } else if (quiz.isTextual()) {
                            com.fasterxml.jackson.databind.JsonNode parsed = objectMapper.readTree(quiz.asText());
                            if (parsed.has("questions")) {
                                totalQuestions = parsed.get("questions").size();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        // 2. Get current attempt's correct answers
        int currentCorrectAnswers = req.getCorrectAnswers() != null ? req.getCorrectAnswers() : 0;

        // 3. Get best score so far (highest correct answers achieved)
        int previousBestScore = enrollment.getCorrectAnswers() != null ? enrollment.getCorrectAnswers() : 0;

        // 4. Calculate EXP gain - only if improved
        int correctAnswersGained = 0;
        if (currentCorrectAnswers > previousBestScore) {
            correctAnswersGained = currentCorrectAnswers - previousBestScore;
            // Update best score
            enrollment.setCorrectAnswers(currentCorrectAnswers);

            // Add EXP to learner (only the improvement delta)
            if (correctAnswersGained > 0) {
                learnerService.addCorrectAnswers(enrollment.getLearner().getId(), correctAnswersGained);
            }
        }
        // If currentCorrectAnswers <= previousBestScore, no EXP gained, best score
        // stays same

        // 5. Update status and progress
        if (totalQuestions > 0 && currentCorrectAnswers >= totalQuestions) {
            // Only set COMPLETED if got ALL questions correct
            enrollment.setStatus(Enrollment.Status.COMPLETED);
            enrollment.setCompletedAt(java.time.LocalDateTime.now());
            enrollment.setProgressPercent(100);
        } else {
            // Not completed yet - stay IN_PROGRESS (or keep COMPLETED if already was)
            if (enrollment.getStatus() != Enrollment.Status.COMPLETED) {
                enrollment.setStatus(Enrollment.Status.IN_PROGRESS);
                if (totalQuestions > 0) {
                    // Progress based on BEST score, not current attempt
                    int bestScore = enrollment.getCorrectAnswers();
                    enrollment.setProgressPercent((bestScore * 100) / totalQuestions);
                }
            }
        }

        // 6. Update other fields
        enrollment.setAttempts(enrollment.getAttempts() + req.getAddAttempt());
        enrollment.setLastActivityAt(java.time.LocalDateTime.now());
        if (req.getScore() != null)
            enrollment.setScore(req.getScore());

        // 7. Calculate earned EXP for this enrollment (cumulative best)
        // Earned EXP = best correct answers achieved
        enrollment.setEarnedExp((long) enrollment.getCorrectAnswers());

        enrollmentRepo.save(enrollment);

        // 8. Return gamification data
        return vn.uth.learningservice.dto.response.GamificationRes.builder()
                .userId(enrollment.getLearner().getId())
                .sourceType("QUIZ")
                .lessonId(enrollment.getLesson().getId())
                .enrollId(enrollment.getId())
                .totalQuiz(totalQuestions)
                .correctAnswer(currentCorrectAnswers)
                .build();
    }

    public long countCompletedByLearner(UUID learnerId) {
        return enrollmentRepo.countCompletedByLearner(learnerId);
    }
}
