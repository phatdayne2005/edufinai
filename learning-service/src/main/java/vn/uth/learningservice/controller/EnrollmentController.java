package vn.uth.learningservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('SCOPE_ROLE_LEARNER')")
    public ResponseEntity<EnrollmentRes> enroll(
            @Valid @RequestBody EnrollmentCreateReq req,
            org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken auth) {

        UUID learnerId = UUID.fromString(auth.getToken().getSubject());
        Learner learner = learnerService.getOrCreate(learnerId);
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
    @PreAuthorize("hasAuthority('SCOPE_ROLE_LEARNER')")
    public ResponseEntity<List<EnrollmentRes>> listMyEnrollments(
            org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken auth) {

        UUID learnerId = UUID.fromString(auth.getToken().getSubject());
        // Ensure learner exists
        learnerService.getById(learnerId);

        List<Enrollment> list = enrollmentService.listByLearner(learnerId);
        return ResponseEntity.ok(list.stream()
                .map(enrollmentMapper::toRes)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{enrollmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_LEARNER')")
    public ResponseEntity<EnrollmentRes> getEnrollment(
            @PathVariable UUID enrollmentId,
            org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken auth) {

        Enrollment enrollment = enrollmentService.getById(enrollmentId);
        UUID userId = UUID.fromString(auth.getToken().getSubject());

        // Check if user is the owner
        if (!enrollment.getLearner().getId().equals(userId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(enrollmentMapper.toRes(enrollment));
    }

    @PutMapping("/{enrollmentId}/progress")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_LEARNER')")
    public ResponseEntity<Void> updateProgress(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody EnrollmentProgressReq req,
            org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken auth) {

        Enrollment enrollment = enrollmentService.getById(enrollmentId);
        UUID userId = UUID.fromString(auth.getToken().getSubject());

        // Check if user is the owner
        if (!enrollment.getLearner().getId().equals(userId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

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
