package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.uth.financeservice.dto.GoalRequestDto;
import vn.uth.financeservice.dto.GoalStatusUpDate;
import vn.uth.financeservice.service.GoalService;
import vn.uth.financeservice.client.AuthServiceClient;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Validated
public class GoalController {

    private final GoalService goalService;
    private final AuthServiceClient authServiceClient;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Validated GoalRequestDto dto) {
        UUID userId = authServiceClient.getCurrentUserId();
        return ResponseEntity.ok(goalService.createGoal(userId, dto));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        UUID userId = authServiceClient.getCurrentUserId();
        // Sử dụng getUserGoalsWithAutoStatusUpdate() để tự động check và update status
        // (COMPLETED nếu đạt mục tiêu, FAILED nếu hết hạn mà chưa đạt)
        return ResponseEntity.ok(goalService.getUserGoalsWithAutoStatusUpdate(userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Validated GoalStatusUpDate dto) {
        UUID userId = authServiceClient.getCurrentUserId();
        return ResponseEntity.ok(goalService.updateStatus(id, dto, userId));
    }
}
