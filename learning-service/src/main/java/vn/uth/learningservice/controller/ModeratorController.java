package vn.uth.learningservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.uth.learningservice.dto.request.LessonModerationReq;
import vn.uth.learningservice.dto.response.LessonRes;
import vn.uth.learningservice.mapper.LessonMapper;
import vn.uth.learningservice.model.Lesson;
import vn.uth.learningservice.model.Moderator;
import vn.uth.learningservice.service.LessonService;
import vn.uth.learningservice.service.ModeratorService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/moderators")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SCOPE_ROLE_MODERATOR', 'SCOPE_ROLE_MOD')")
public class ModeratorController {

    private final ModeratorService moderatorService;
    private final LessonService lessonService;
    private final LessonMapper lessonMapper;
    private final vn.uth.learningservice.service.UserService userService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Moderator>> listAll() {
        return ResponseEntity.ok(moderatorService.listAll());
    }

    @GetMapping("/lessons")
    public ResponseEntity<List<LessonRes>> listByStatus(
            @RequestParam(value = "status", required = false) Lesson.Status status) {

        var userInfo = userService.getMyInfo();
        UUID moderatorId = userInfo.getId();
        moderatorService.getOrCreate(moderatorId); // Ensure moderator exists

        List<Lesson> lessons;
        if (status != null) {
            // Nếu có status thì lọc theo status (trừ DRAFT nếu user cố tình truyền vào)
            if (status == Lesson.Status.DRAFT) {
                return ResponseEntity.status(403).build(); // Mod không được xem DRAFT
            }
            lessons = lessonService.listByStatus(status);
        } else {
            // Nếu không truyền status, mặc định lấy PENDING
            lessons = lessonService.listByStatus(Lesson.Status.PENDING);
        }
        return ResponseEntity.ok(mapToResList(lessons));
    }

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<LessonRes> viewLessonById(
            @PathVariable("lessonId") UUID lessonId) {

        var userInfo = userService.getMyInfo();
        UUID moderatorId = userInfo.getId();
        moderatorService.getOrCreate(moderatorId); // Verify moderator exists

        Lesson lesson = lessonService.getById(lessonId);

        // Mod không được xem bài DRAFT
        if (lesson.getStatus() == Lesson.Status.DRAFT) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(lessonMapper.toRes(lesson, objectMapper));
    }

    @PostMapping("/lessons/{lessonId}/decision")
    public ResponseEntity<LessonRes> moderateLesson(
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody LessonModerationReq request) {

        var userInfo = userService.getMyInfo();
        UUID moderatorId = userInfo.getId();
        moderatorService.getById(moderatorId); // Ensure moderator exists

        Lesson updated = lessonService.moderateLesson(moderatorId, lessonId, request.getStatus(),
                request.getCommentByMod());
        return ResponseEntity.ok(lessonMapper.toRes(updated, objectMapper));
    }

    private List<LessonRes> mapToResList(List<Lesson> lessons) {
        return lessons.stream()
                .map(lesson -> lessonMapper.toRes(lesson, objectMapper))
                .collect(Collectors.toList());
    }
}
