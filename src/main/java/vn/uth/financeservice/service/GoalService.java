package vn.uth.financeservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.GoalRequestDto;
import vn.uth.financeservice.dto.GoalStatusUpDate;
import vn.uth.financeservice.entity.Goal;
import vn.uth.financeservice.entity.GoalStatus;
import vn.uth.financeservice.repository.GoalRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;

    @Transactional
    public Goal createGoal(UUID userId, GoalRequestDto request) {
        Goal g = new Goal();
        g.setGoalId(UUID.randomUUID());
        g.setUserId(userId);
        g.setTitle(request.getTitle());
        g.setAmount(request.getAmount());
        g.setStartAt(request.getStartAt() != null ? request.getStartAt() : LocalDateTime.now());
        g.setEndAt(request.getEndAt());
        g.setStatus(GoalStatus.ACTIVE);
        g.setNewStatus(GoalStatus.ACTIVE); // Set newStatus để tránh lỗi NOT NULL
        g.setUpdatedAt(LocalDateTime.now()); // tránh lỗi NOT NULL
        return goalRepository.save(g);
    }

    @Transactional
    public Goal updateStatus(UUID goalId, GoalStatusUpDate dto, UUID userId) {
        Goal g = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        if (!g.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }

        // Convert String -> Enum
        GoalStatus newStatus = GoalStatus.valueOf(dto.getStatus().toUpperCase());
        g.setStatus(newStatus);
        g.setNewStatus(newStatus); // Cập nhật cả newStatus
        g.setUpdatedAt(LocalDateTime.now()); // Cập nhật updatedAt

        return goalRepository.save(g);
    }

    /**
     * Kiểm tra và cập nhật status của goal dựa trên savedAmount và endAt
     * Logic:
     * - Nếu savedAmount >= amount → COMPLETED
     * - Nếu endAt < now và savedAmount < amount → FAILED
     * - Còn lại → giữ nguyên status (ACTIVE)
     */
    @Transactional
    public Goal checkAndUpdateGoalStatus(Goal goal) {
        if (goal == null) {
            return null;
        }

        // Chỉ check và update nếu goal đang ở trạng thái ACTIVE
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            return goal;
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal savedAmount = goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO;
        BigDecimal targetAmount = goal.getAmount() != null ? goal.getAmount() : BigDecimal.ZERO;
        LocalDateTime endAt = goal.getEndAt();

        GoalStatus newStatus = goal.getStatus();

        // Check: Nếu đã đạt mục tiêu (savedAmount >= amount) → COMPLETED
        if (savedAmount.compareTo(targetAmount) >= 0 && targetAmount.compareTo(BigDecimal.ZERO) > 0) {
            newStatus = GoalStatus.COMPLETED;
        }
        // Check: Nếu hết hạn mà chưa đạt mục tiêu → FAILED
        else if (endAt != null && endAt.isBefore(now) && savedAmount.compareTo(targetAmount) < 0) {
            newStatus = GoalStatus.FAILED;
        }
        // Còn lại → giữ ACTIVE

        // Chỉ update nếu status thay đổi
        if (newStatus != goal.getStatus()) {
            goal.setStatus(newStatus);
            goal.setNewStatus(newStatus);
            goal.setUpdatedAt(now);
            return goalRepository.save(goal);
        }

        return goal;
    }

    @Transactional(readOnly = true)
    public List<Goal> getUserGoals(UUID userId) {
        // Method này chỉ đọc, không update status
        // Sử dụng getUserGoalsWithAutoStatusUpdate() nếu muốn tự động update status
        return goalRepository.findByUserId(userId);
    }

    /**
     * Lấy danh sách goals và tự động check/update status
     * Sử dụng method này thay vì getUserGoals() nếu muốn tự động update status
     */
    @Transactional
    public List<Goal> getUserGoalsWithAutoStatusUpdate(UUID userId) {
        List<Goal> goals = goalRepository.findByUserId(userId);
        
        // Check và update status cho từng goal
        return goals.stream()
                .map(this::checkAndUpdateGoalStatus)
                .collect(Collectors.toList());
    }
}
