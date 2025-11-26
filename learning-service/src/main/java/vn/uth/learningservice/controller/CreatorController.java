package vn.uth.learningservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.uth.learningservice.dto.request.LessonCreateReq;
import vn.uth.learningservice.dto.response.CreatorRes;
import vn.uth.learningservice.dto.response.LessonRes;
import vn.uth.learningservice.mapper.CreatorMapper;
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
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorService creatorService;
    private final CreatorMapper mapper;
    private final LessonService lessonService;
    private final LessonMapper lessonMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    // GET /api/creators/me - Lấy thông tin của chính creator đang đăng nhập
    @GetMapping("/me")
    public ResponseEntity<CreatorRes> getMe() {
        var userInfo = userService.getMyInfo();
        UUID id = userInfo.getId();
        Creator creator = creatorService.getOrCreate(id);
        CreatorRes dto = mapper.toDto(creator);
        long totalLessons = creatorService.countLessons(id);
        dto.setTotalLessons(totalLessons);
        return ResponseEntity.ok(dto);
    }

    // GET /api/creators/me/lessons - Lấy danh sách bài học của creator đang đăng
    // nhập
    @GetMapping("/me/lessons")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_CREATOR')")
    public ResponseEntity<List<LessonRes>> getMyLessons() {
        var userInfo = userService.getMyInfo();
        UUID creatorId = userInfo.getId();

        // Ensure creator exists
        creatorService.getOrCreate(creatorId);

        List<Lesson> lessons = lessonService.listByCreator(creatorId);
        return ResponseEntity.ok(lessons.stream()
                .map(lesson -> lessonMapper.toRes(lesson, objectMapper))
                .collect(Collectors.toList()));
    }

    // GET /api/creators/{id} - Lấy thông tin chi tiết của một creator theo ID
    @GetMapping("/{id}")
    public ResponseEntity<CreatorRes> getById(@PathVariable("id") UUID id) {
        Creator creator = creatorService.getById(id);
        CreatorRes dto = mapper.toDto(creator);
        // Set totalLessons từ service
        long totalLessons = creatorService.countLessons(id);
        dto.setTotalLessons(totalLessons);
        return ResponseEntity.ok(dto);
    }

    // GET /api/creators - Lấy danh sách tất cả creators
    @GetMapping
    public ResponseEntity<List<CreatorRes>> listAll() {
        List<Creator> creators = creatorService.listAll();
        List<CreatorRes> dtoList = creators.stream()
                .map(creator -> {
                    CreatorRes dto = mapper.toDto(creator);
                    long totalLessons = creatorService.countLessons(creator.getId());
                    dto.setTotalLessons(totalLessons);
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

}
