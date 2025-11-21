package vn.uth.learningservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
public class ModeratorController {

    private final ModeratorService moderatorService;
    private final LessonService lessonService;
    private final LessonMapper lessonMapper;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Moderator>> listAll() {
        return ResponseEntity.ok(moderatorService.listAll());
    }

    @GetMapping("/{moderatorId}/lessons/pending")
    public ResponseEntity<List<LessonRes>> listPending(@PathVariable("moderatorId") UUID moderatorId) {
        moderatorService.getById(moderatorId);
        List<Lesson> pending = moderatorService.listPending(moderatorId);
        return ResponseEntity.ok(mapToResList(pending));
    }

    @GetMapping("/{moderatorId}/lessons/{lessonId}")
    public ResponseEntity<LessonRes> viewLessonById(
            @PathVariable("moderatorId") UUID moderatorId,
            @PathVariable("lessonId") UUID lessonId) {
        moderatorService.getById(moderatorId);
        Lesson lesson = lessonService.getById(lessonId);
        // Bỏ check assignment, moderator có thể xem bất kỳ bài nào
        return ResponseEntity.ok(lessonMapper.toRes(lesson, objectMapper));
    }

    @PostMapping("/{moderatorId}/lessons/{lessonId}/decision")
    public ResponseEntity<LessonRes> moderateLesson(
            @PathVariable("moderatorId") UUID moderatorId,
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody LessonModerationReq request) {
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
