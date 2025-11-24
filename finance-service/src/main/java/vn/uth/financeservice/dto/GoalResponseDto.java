package vn.uth.financeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.uth.financeservice.entity.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponseDto {
    private UUID goalId;
    private UUID userId;
    private String title;
    private BigDecimal amount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private GoalStatus status;
    private LocalDateTime updatedAt;
    private GoalStatus newStatus;
    private BigDecimal savedAmount;
}

