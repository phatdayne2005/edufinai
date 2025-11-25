package vn.uth.learningservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.learningservice.dto.response.GamificationRes;
import vn.uth.learningservice.model.Enrollment;
import vn.uth.learningservice.service.EnrollmentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class GamificationController {

    private final EnrollmentService enrollmentService;

    @GetMapping("/gamify-response")
    public ResponseEntity<GamificationRes> getGamifyResponse(@RequestParam("enrollmentId") UUID enrollmentId) {
        Enrollment enrollment = enrollmentService.getById(enrollmentId);

        GamificationRes res = GamificationRes.builder()
                .userId(enrollment.getLearner().getId())
                .sourceType("QUIZ")
                .lessonId(enrollment.getLesson().getId())
                .enrollId(enrollment.getId())
                .totalQuiz(
                        enrollment.getLesson().getTotalQuestions() != null ? enrollment.getLesson().getTotalQuestions()
                                : 0)
                .correctAnswer(enrollment.getCorrectAnswers())
                .build();

        return ResponseEntity.ok(res);
    }
}
