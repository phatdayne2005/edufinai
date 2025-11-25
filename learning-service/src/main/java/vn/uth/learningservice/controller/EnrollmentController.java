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
import vn.uth.learningservice.model.*;
import vn.uth.learningservice.service.*;

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
        private final UserService userService;

        @PostMapping
        @PreAuthorize("hasAuthority('SCOPE_ROLE_LEARNER')")
        public ResponseEntity<EnrollmentRes> enroll(
                        @Valid @RequestBody EnrollmentCreateReq req) {

                var userInfo = userService.getMyInfo();
                UUID learnerId = userInfo.getId();
                Learner learner = learnerService.getOrCreate(learnerId);
                Lesson lesson = lessonService.getById(req.getLessonId());

                // Validate learner level vs lesson difficulty
                if (!canEnrollInLesson(learner.getLevel(), lesson.getDifficulty())) {
                        throw new IllegalArgumentException(
                                        String.format("Learner level %s is not sufficient to enroll in lesson with difficulty %s. Required level: %s",
                                                        learner.getLevel(),
                                                        lesson.getDifficulty(),
                                                        getRequiredLevelForDifficulty(lesson.getDifficulty())));
                }

                Enrollment newEnroll = new Enrollment();
                newEnroll.setLearner(learner);
                newEnroll.setLesson(lesson);
                newEnroll.setStatus(Enrollment.Status.IN_PROGRESS);
                newEnroll.setStartedAt(java.time.LocalDateTime.now());
                newEnroll.setLastActivityAt(java.time.LocalDateTime.now());

                Enrollment saved = enrollmentService.enrollIfAbsent(newEnroll);
                return ResponseEntity.ok(enrollmentMapper.toRes(saved));
        }

        /**
         * Check if a learner with given level can enroll in a lesson with given
         * difficulty
         * BEGINNER -> BASIC only
         * INTERMEDIATE -> BASIC, INTERMEDIATE
         * ADVANCED -> all difficulties
         */
        private boolean canEnrollInLesson(Learner.Level learnerLevel, Lesson.Difficulty lessonDifficulty) {
                return switch (learnerLevel) {
                        case BEGINNER -> lessonDifficulty == Lesson.Difficulty.BASIC;
                        case INTERMEDIATE -> lessonDifficulty == Lesson.Difficulty.BASIC
                                        || lessonDifficulty == Lesson.Difficulty.INTERMEDIATE;
                        case ADVANCED -> true; // Can enroll in any difficulty
                };
        }

        /**
         * Get the minimum required learner level for a lesson difficulty
         */
        private Learner.Level getRequiredLevelForDifficulty(Lesson.Difficulty difficulty) {
                return switch (difficulty) {
                        case BASIC -> Learner.Level.BEGINNER;
                        case INTERMEDIATE -> Learner.Level.INTERMEDIATE;
                        case ADVANCED -> Learner.Level.ADVANCED;
                };
        }

        @GetMapping
        @PreAuthorize("hasAuthority('SCOPE_ROLE_LEARNER')")
        public ResponseEntity<List<EnrollmentRes>> listMyEnrollments() {

                var userInfo = userService.getMyInfo();
                UUID learnerId = userInfo.getId();
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
                        @PathVariable UUID enrollmentId) {

                Enrollment enrollment = enrollmentService.getById(enrollmentId);
                var userInfo = userService.getMyInfo();
                UUID userId = userInfo.getId();

                // Check if user is the owner
                if (!enrollment.getLearner().getId().equals(userId)) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }

                return ResponseEntity.ok(enrollmentMapper.toRes(enrollment));
        }

        @PutMapping("/{enrollmentId}/progress")
        @PreAuthorize("hasAuthority('SCOPE_ROLE_LEARNER')")
        public ResponseEntity<vn.uth.learningservice.dto.response.GamificationRes> updateProgress(
                        @PathVariable UUID enrollmentId,
                        @Valid @RequestBody EnrollmentProgressReq req) {

                Enrollment enrollment = enrollmentService.getById(enrollmentId);
                var userInfo = userService.getMyInfo();
                UUID userId = userInfo.getId();

                // Check if user is the owner
                if (!enrollment.getLearner().getId().equals(userId)) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }

                var res = enrollmentService.updateProgress(enrollmentId, req);
                return ResponseEntity.ok(res);
        }

        // ========== Context-Aware Endpoints (Slug-based) ==========

        /**
         * Get my enrollment for a specific lesson using lesson slug.
         * This eliminates the need to expose enrollment ID in URLs.
         */
        @GetMapping("/lessons/{slug}/my-enrollment")
        @PreAuthorize("hasAuthority('SCOPE_ROLE_LEARNER')")
        public ResponseEntity<EnrollmentRes> getMyEnrollmentForLesson(
                        @PathVariable("slug") String slug) {

                var userInfo = userService.getMyInfo();
                UUID learnerId = userInfo.getId();

                // Find lesson by slug
                Lesson lesson = lessonService.findBySlug(slug)
                                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with slug: " + slug));

                // Find enrollment for this learner and lesson
                Enrollment enrollment = enrollmentService.findByLearnerAndLesson(learnerId, lesson.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Not enrolled in this lesson"));

                return ResponseEntity.ok(enrollmentMapper.toRes(enrollment));
        }

        /**
         * Update progress for my enrollment using lesson slug.
         * Frontend only needs to know the lesson slug, not the enrollment ID.
         */
        @PutMapping("/lessons/{slug}/my-enrollment/progress")
        @PreAuthorize("hasAuthority('SCOPE_ROLE_LEARNER')")
        public ResponseEntity<vn.uth.learningservice.dto.response.GamificationRes> updateMyEnrollmentProgress(
                        @PathVariable("slug") String slug,
                        @Valid @RequestBody EnrollmentProgressReq req) {

                var userInfo = userService.getMyInfo();
                UUID learnerId = userInfo.getId();

                // Find lesson by slug
                Lesson lesson = lessonService.findBySlug(slug)
                                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with slug: " + slug));

                // Find enrollment for this learner and lesson
                Enrollment enrollment = enrollmentService.findByLearnerAndLesson(learnerId, lesson.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Not enrolled in this lesson"));

                // Update progress
                var res = enrollmentService.updateProgress(enrollment.getId(), req);

                return ResponseEntity.ok(res);
        }
}
