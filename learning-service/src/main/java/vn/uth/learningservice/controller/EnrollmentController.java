package vn.uth.learningservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.learningservice.dto.request.EnrollmentCreateReq;
import vn.uth.learningservice.dto.request.EnrollmentProgressReq;
import vn.uth.learningservice.dto.response.EnrollmentRes;
import vn.uth.learningservice.mapper.EnrollmentMapper;
import vn.uth.learningservice.model.Enrollment;
import vn.uth.learningservice.model.Learner;
import vn.uth.learningservice.model.Lesson;
import vn.uth.learningservice.service.EnrollmentService;
import vn.uth.learningservice.service.LearnerService;
import vn.uth.learningservice.service.LessonService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final LearnerService learnerService;
    private final LessonService lessonService;
    private final EnrollmentMapper enrollmentMapper;

    @PostMapping
    public ResponseEntity<EnrollmentRes> enroll(
            @RequestParam("learnerId") UUID learnerId,
            @Valid @RequestBody EnrollmentCreateReq req) {

        Learner learner = learnerService.getById(learnerId);
        Lesson lesson = lessonService.getById(req.getLessonId());

        Enrollment newEnroll = new Enrollment();
        newEnroll.setLearner(learner);
        newEnroll.setLesson(lesson);
        newEnroll.setStatus(Enrollment.Status.IN_PROGRESS);
        newEnroll.setStartedAt(java.time.LocalDateTime.now());
        newEnroll.setLastActivityAt(java.time.LocalDateTime.now());

        Enrollment saved = enrollmentService.enrollIfAbsent(newEnroll);
        return ResponseEntity.ok(enrollmentMapper.toRes(saved));
    }

    @GetMapping
    public ResponseEntity<List<EnrollmentRes>> listMyEnrollments(@RequestParam("learnerId") UUID learnerId) {
        // Ensure learner exists
        learnerService.getById(learnerId);

        List<Enrollment> list = enrollmentService.listByLearner(learnerId);
        return ResponseEntity.ok(list.stream()
                .map(enrollmentMapper::toRes)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{enrollmentId}")
    public ResponseEntity<EnrollmentRes> getEnrollment(@PathVariable UUID enrollmentId) {
        Enrollment enrollment = enrollmentService.getById(enrollmentId);
        return ResponseEntity.ok(enrollmentMapper.toRes(enrollment));
    }

    @PutMapping("/{enrollmentId}/progress")
    public ResponseEntity<Void> updateProgress(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody EnrollmentProgressReq req) {

        // Map DTO status to Entity status
        Enrollment.Status status = Enrollment.Status.valueOf(req.getStatus().name());

        enrollmentService.updateProgress(
                enrollmentId,
                status,
                req.getProgressPercent(),
                req.getScore(),
                req.getAddAttempt());

        return ResponseEntity.ok().build();
    }
}
