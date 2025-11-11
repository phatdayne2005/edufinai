package vn.uth.financeservice.service;

import vn.uth.financeservice.dto.GoalRequestDto;
import vn.uth.financeservice.entity.Goal;
import vn.uth.financeservice.entity.GoalStatus;
import vn.uth.financeservice.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalService {
    private final GoalRepository goalRepository;

    @Transactional
    public Goal createGoal(GoalRequestDto request, UUID userId) {
        Goal g = new Goal();
        g.setGoalId(UUID.randomUUID());
        g.setUserId(userId);
        g.setTitle(request.getTitle());
        g.setAmount(request.getAmount());
        g.setStartAt(request.getStartAt() != null ? request.getStartAt() : LocalDateTime.now());
        g.setEndAt(request.getEndAt());
        g.setStatus(GoalStatus.ACTIVE);
        return goalRepository.save(g);
    }

    @Transactional
    public void updateGoalStatus(UUID goalId, String status, UUID userId) {
        Goal g = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!g.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }
        g.setStatus(GoalStatus.valueOf(status));
        goalRepository.save(g);
    }

    @Transactional(readOnly = true)
    public List<Goal> getUserGoals(UUID userId) {
        return goalRepository.findByUserId(userId);
    }
}
