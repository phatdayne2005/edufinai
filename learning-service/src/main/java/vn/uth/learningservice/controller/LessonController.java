package vn.uth.learningservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.uth.learningservice.dto.request.LessonCreateReq;
import vn.uth.learningservice.dto.request.LessonUpdateReq;
import vn.uth.learningservice.dto.response.LessonRes;
import vn.uth.learningservice.mapper.LessonMapper;
import vn.uth.learningservice.model.Creator;
import vn.uth.learningservice.model.Lesson;
import vn.uth.learningservice.service.CreatorService;
import vn.uth.learningservice.service.LessonService;
import vn.uth.learningservice.service.UserService;

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
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<LessonRes>> findAll() {
        List<Lesson> lessons = lessonService.listAll();
        return ResponseEntity.ok(mapToResList(lessons));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonRes> findById(@PathVariable("id") UUID id) {
        Lesson lesson = lessonService.getById(id);
        return ResponseEntity.ok(lessonMapper.toRes(lesson, objectMapper));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<LessonRes> findBySlug(@PathVariable("slug") String slug) {
        Lesson lesson = lessonService.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with slug: " + slug));
        return ResponseEntity.ok(lessonMapper.toRes(lesson, objectMapper));
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

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ROLE_CREATOR')")
    public ResponseEntity<LessonRes> createLesson(
            @Valid @RequestBody LessonCreateReq request) {

        var userInfo = userService.getMyInfo();
        UUID creatorId = userInfo.getId();
        Creator creator = creatorService.getOrCreate(creatorId);

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
    @PreAuthorize("hasAuthority('SCOPE_ROLE_CREATOR')")
    public ResponseEntity<LessonRes> updateLesson(
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody LessonUpdateReq request) {

        Lesson existing = lessonService.getById(lessonId);
        var userInfo = userService.getMyInfo();
        UUID userId = userInfo.getId();

        if (!existing.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        lessonMapper.patch(existing, request, objectMapper);
        existing.setStatus(Lesson.Status.DRAFT);
        Lesson updated = lessonService.save(existing);

        return ResponseEntity.ok(lessonMapper.toRes(updated, objectMapper));
    }

    @PutMapping("/slug/{slug}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_CREATOR')")
    public ResponseEntity<LessonRes> updateLessonBySlug(
            @PathVariable("slug") String slug,
            @Valid @RequestBody LessonUpdateReq request) {

        Lesson existing = lessonService.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with slug: " + slug));

        var userInfo = userService.getMyInfo();
        UUID userId = userInfo.getId();

        if (!existing.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        lessonMapper.patch(existing, request, objectMapper);
        existing.setStatus(Lesson.Status.DRAFT);
        Lesson updated = lessonService.save(existing);

        return ResponseEntity.ok(lessonMapper.toRes(updated, objectMapper));
    }

    @PutMapping("/{lessonId}/submit")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_CREATOR')")
    public ResponseEntity<LessonRes> submitLesson(
            @PathVariable("lessonId") UUID lessonId) {

        Lesson lesson = lessonService.getById(lessonId);
        var userInfo = userService.getMyInfo();
        UUID userId = userInfo.getId();

        if (!lesson.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Lesson submitted = lessonService.submitLesson(lessonId);
        return ResponseEntity.ok(lessonMapper.toRes(submitted, objectMapper));
    }

    @PutMapping("/slug/{slug}/submit")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_CREATOR')")
    public ResponseEntity<LessonRes> submitLessonBySlug(
            @PathVariable("slug") String slug) {

        Lesson lesson = lessonService.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with slug: " + slug));

        var userInfo = userService.getMyInfo();
        UUID userId = userInfo.getId();

        if (!lesson.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Lesson submitted = lessonService.submitLesson(lesson.getId());
        return ResponseEntity.ok(lessonMapper.toRes(submitted, objectMapper));
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_CREATOR')")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable("lessonId") UUID lessonId) {

        Lesson lesson = lessonService.getById(lessonId);
        var userInfo = userService.getMyInfo();
        UUID userId = userInfo.getId();

        if (!lesson.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        lessonService.delete(lessonId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/slug/{slug}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_CREATOR')")
    public ResponseEntity<Void> deleteLessonBySlug(
            @PathVariable("slug") String slug) {

        Lesson lesson = lessonService.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with slug: " + slug));

        var userInfo = userService.getMyInfo();
        UUID userId = userInfo.getId();

        if (!lesson.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        lessonService.delete(lesson.getId());
        return ResponseEntity.noContent().build();
    }

    private List<LessonRes> mapToResList(List<Lesson> lessons) {
        return lessons.stream()
                .map(lesson -> lessonMapper.toRes(lesson, objectMapper))
                .collect(Collectors.toList());
    }
}
