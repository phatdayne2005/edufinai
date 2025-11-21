package vn.uth.financeservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goal")
@Getter @Setter
public class Goal {
    @Id
    @Column(name = "goal_id", columnDefinition = "BINARY(16)")
    private UUID goalId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GoalStatus status; // ACTIVE, COMPLETED, FAILED

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GoalStatus newStatus;

    @Column(name = "saved_amount", nullable = false)
    private BigDecimal savedAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "goal", fetch = FetchType.LAZY)
    private List<Transaction> transactions;
}


