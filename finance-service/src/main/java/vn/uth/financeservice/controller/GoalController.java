package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<?> create(@RequestBody @Validated GoalRequestDto dto,
                                    Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(goalService.createGoal(userId, dto));
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        // Sử dụng getUserGoalsWithAutoStatusUpdate() để tự động check và update status
        // (COMPLETED nếu đạt mục tiêu, FAILED nếu hết hạn mà chưa đạt)
        return ResponseEntity.ok(goalService.getUserGoalsWithAutoStatusUpdate(userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Validated GoalStatusUpDate dto,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(goalService.updateStatus(id, dto, userId));
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Unauthenticated request");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID uuid) {
            return uuid;
        }
        if (principal instanceof String value) {
            return UUID.fromString(value);
        }
        throw new RuntimeException("Invalid authentication principal");
    }
}
