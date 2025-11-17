package vn.uth.financeservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
public class Transaction {
    @Id
    @Column(name = "transaction_id", columnDefinition = "BINARY(16)")
    private UUID transactionId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type; // INCOME or EXPENSE

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String name; // Tên giao dịch

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", columnDefinition = "BINARY(16)")
    private Category category;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate; // Ngày giao dịch

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", columnDefinition = "BINARY(16)")
    private Goal goal;

    @Column(nullable = false, length = 10)
    private String status; // ACTIVE or DELETED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}


