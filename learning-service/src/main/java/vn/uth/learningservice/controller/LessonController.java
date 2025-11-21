package vn.uth.learningservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.learningservice.dto.request.LessonCreateReq;
import vn.uth.learningservice.dto.request.LessonUpdateReq;
import vn.uth.learningservice.dto.response.LessonRes;
import vn.uth.learningservice.mapper.LessonMapper;
import vn.uth.learningservice.model.Creator;
import vn.uth.learningservice.model.Lesson;
import vn.uth.learningservice.service.CreatorService;
import vn.uth.learningservice.service.LessonService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;
    private final CreatorService creatorService;
    private final LessonMapper lessonMapper;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<LessonRes>> findAll() {
        List<Lesson> lessons = lessonService.listAll();
        return ResponseEntity.ok(mapToResList(lessons));
    }

    @GetMapping("/tags/{tag}")
    public ResponseEntity<List<LessonRes>> findByTag(@PathVariable("tag") Lesson.Tag tag) {
        List<Lesson> lessons = lessonService.listByTag(tag);
        return ResponseEntity.ok(mapToResList(lessons));
    }

    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<List<LessonRes>> findByDifficulty(@PathVariable("difficulty") Lesson.Difficulty difficulty) {
        List<Lesson> lessons = lessonService.listByDifficulty(difficulty);
        return ResponseEntity.ok(mapToResList(lessons));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LessonRes>> findByStatus(@PathVariable("status") Lesson.Status status) {
        List<Lesson> lessons = lessonService.listByStatus(status);
        return ResponseEntity.ok(mapToResList(lessons));
    }

    @PostMapping("/creator/{creatorId}")
    public ResponseEntity<LessonRes> createLesson(
            @PathVariable("creatorId") UUID creatorId,
            @Valid @RequestBody LessonCreateReq request) {
        Creator creator = creatorService.getById(creatorId);
        Lesson lesson = lessonMapper.toEntity(request, objectMapper);
        lesson.setCreator(creator);

        String slug = Lesson.slugify(request.getTitle());
        String finalSlug = slug;
        int counter = 1;
        while (lessonService.existsBySlug(finalSlug)) {
            finalSlug = slug + "-" + counter;
            counter++;
        }
        lesson.setSlug(finalSlug);

        LocalDateTime now = LocalDateTime.now();
        lesson.setCreatedAt(now);
        lesson.setUpdatedAt(now);
        lesson.setStatus(Lesson.Status.DRAFT);

        Lesson savedLesson = lessonService.create(lesson);
        LessonRes response = lessonMapper.toRes(savedLesson, objectMapper);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{lessonId}")
    public ResponseEntity<LessonRes> updateLesson(
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody LessonUpdateReq request) {
        Lesson existing = lessonService.getById(lessonId);

        // Map changes from req to existing entity
        lessonMapper.patch(existing, request, objectMapper);

        // Logic mới: Khi update nội dung, status luôn quay về DRAFT
        existing.setStatus(Lesson.Status.DRAFT);

        // Save changes
        Lesson updated = lessonService.save(existing);

        LessonRes response = lessonMapper.toRes(updated, objectMapper);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{lessonId}/submit")
    public ResponseEntity<LessonRes> submitLesson(@PathVariable("lessonId") UUID lessonId) {
        Lesson submitted = lessonService.submitLesson(lessonId);
        return ResponseEntity.ok(lessonMapper.toRes(submitted, objectMapper));
    }

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@PathVariable("lessonId") UUID lessonId) {
        lessonService.delete(lessonId);
        return ResponseEntity.noContent().build();
    }

    private List<LessonRes> mapToResList(List<Lesson> lessons) {
        return lessons.stream()
                .map(lesson -> lessonMapper.toRes(lesson, objectMapper))
                .collect(Collectors.toList());
    }
}
