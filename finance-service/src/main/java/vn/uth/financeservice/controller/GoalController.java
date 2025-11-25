package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.uth.financeservice.dto.GoalRequestDto;
import vn.uth.financeservice.dto.GoalResponseDto;
import vn.uth.financeservice.dto.GoalWithdrawRequestDto;
import vn.uth.financeservice.dto.TransactionResponseDto;
import vn.uth.financeservice.dto.GoalTransactionHistoryDto;
import vn.uth.financeservice.entity.Goal;
import vn.uth.financeservice.entity.Transaction;
import vn.uth.financeservice.service.GoalService;
import vn.uth.financeservice.client.AuthServiceClient;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Validated
public class GoalController {

    private final GoalService goalService;
    private final AuthServiceClient authServiceClient;

    @PostMapping
    public ResponseEntity<GoalResponseDto> create(@RequestBody @Validated GoalRequestDto dto) {
        UUID userId = authServiceClient.getCurrentUserId();
        Goal goal = goalService.createGoal(userId, dto);
        return ResponseEntity.ok(toGoalResponseDto(goal));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponseDto>> list() {
        UUID userId = authServiceClient.getCurrentUserId();
        // Sử dụng getUserGoalsWithAutoStatusUpdate() để tự động check và update status
        // (COMPLETED nếu đạt mục tiêu, FAILED nếu hết hạn mà chưa đạt)
        List<Goal> goals = goalService.getUserGoalsWithAutoStatusUpdate(userId);
        List<GoalResponseDto> response = goals.stream()
                .map(this::toGoalResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm-completion")
    public ResponseEntity<GoalResponseDto> confirmCompletion(@PathVariable UUID id) {
        UUID userId = authServiceClient.getCurrentUserId();
        Goal goal = goalService.confirmCompletion(id, userId);
        return ResponseEntity.ok(toGoalResponseDto(goal));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<TransactionResponseDto> withdraw(
            @PathVariable UUID id,
            @RequestBody @Validated GoalWithdrawRequestDto dto) {
        UUID userId = authServiceClient.getCurrentUserId();
        Transaction transaction = goalService.withdrawFromGoal(id, dto, userId);
        TransactionResponseDto response = toTransactionResponseDto(transaction);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        UUID userId = authServiceClient.getCurrentUserId();
        goalService.deleteGoal(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Lấy lịch sử giao dịch của goal
     * @param id Goal ID
     * @return Lịch sử giao dịch bao gồm thông tin goal và danh sách transactions
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<GoalTransactionHistoryDto> getGoalTransactionHistory(@PathVariable UUID id) {
        UUID userId = authServiceClient.getCurrentUserId();
        GoalTransactionHistoryDto history = goalService.getGoalTransactionHistory(id, userId);
        return ResponseEntity.ok(history);
    }

    private GoalResponseDto toGoalResponseDto(Goal goal) {
        return new GoalResponseDto(
                goal.getGoalId(),
                goal.getUserId(),
                goal.getTitle(),
                goal.getAmount(),
                goal.getStartAt(),
                goal.getEndAt(),
                goal.getStatus(),
                goal.getUpdatedAt(),
                goal.getNewStatus(),
                goal.getSavedAmount()
        );
    }

    private TransactionResponseDto toTransactionResponseDto(Transaction t) {
        return new TransactionResponseDto(
                t.getTransactionId(),
                t.getType(),
                t.getName(),
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getNote(),
                t.getAmount(),
                t.getTransactionDate(),
                t.getGoal() != null ? t.getGoal().getGoalId() : null
        );
    }
}
