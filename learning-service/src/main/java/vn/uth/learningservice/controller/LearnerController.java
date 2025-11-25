package vn.uth.learningservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.learningservice.dto.response.LearnerRes;
import vn.uth.learningservice.dto.shared.LearnerLevel;
import vn.uth.learningservice.mapper.LearnerMapper;
import vn.uth.learningservice.model.Learner;
import vn.uth.learningservice.service.LearnerService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/learners")
@RequiredArgsConstructor
public class LearnerController {

    private final LearnerService learnerService;
    private final LearnerMapper mapper;
    private final vn.uth.learningservice.service.UserService userService;

    // GET /api/learners/me - Lấy thông tin của chính learner đang đăng nhập
    @GetMapping("/me")
    public ResponseEntity<LearnerRes> getMe() {
        var userInfo = userService.getMyInfo();
        UUID id = userInfo.getId();
        Learner learner = learnerService.getOrCreate(id);
        return ResponseEntity.ok(mapper.toDto(learner));
    }

    // GET /api/learners/{id} -Lấy thông tin chi tiết của một learner theo ID
    @GetMapping("/{id}")
    public ResponseEntity<LearnerRes> getById(@PathVariable("id") UUID id) {
        Learner learner = learnerService.getById(id);
        return ResponseEntity.ok(mapper.toDto(learner));
    }

    // GET /api/learners - Lấy danh sách tất cả learners
    @GetMapping
    public ResponseEntity<List<LearnerRes>> listAll() {
        List<Learner> learners = learnerService.listAll();
        List<LearnerRes> dtoList = learners.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    // GET /api/learners/level/{level} - Lấy danh sách learners theo level
    // @param level Level của learner (BEGINNER, INTERMEDIATE, ADVANCED)
    @GetMapping("/level/{level}")
    public ResponseEntity<List<LearnerRes>> listByLevel(@PathVariable("level") LearnerLevel level) {
        Learner.Level modelLevel = mapper.toModelLevel(level);
        List<Learner> learners = learnerService.listByLevel(modelLevel);
        List<LearnerRes> dtoList = learners.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }
}
