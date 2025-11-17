package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.uth.financeservice.dto.GoalRequestDto;
import vn.uth.financeservice.dto.GoalStatusUpDate;
import vn.uth.financeservice.service.GoalService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Validated
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Validated GoalRequestDto dto) {
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes());  // sau này sẽ thay bằng JWT
        return ResponseEntity.ok(goalService.createGoal(userId, dto));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes());  // sau này sẽ thay bằng JWT
        // Sử dụng getUserGoalsWithAutoStatusUpdate() để tự động check và update status
        // (COMPLETED nếu đạt mục tiêu, FAILED nếu hết hạn mà chưa đạt)
        return ResponseEntity.ok(goalService.getUserGoalsWithAutoStatusUpdate(userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Validated GoalStatusUpDate dto
    ) {
        return ResponseEntity.ok(goalService.updateStatus(id, dto));
    }
}
