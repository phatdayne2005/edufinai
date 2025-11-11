package vn.uth.financeservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


    @Entity
    @Table(name = "expense")
    @Getter
    @Setter
    public class Expense {
        @Id
        @Column(name = "expense_id")
        private UUID expenseId;

        @Column(name = "user_id", nullable = false)
        private UUID userId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 10)
        private ExpenseType type; // INCOME or EXPENSE

        @Column(nullable = false)
        private BigDecimal amount;

        @Column(nullable = false, length = 100)
        private String category;

        @Column(columnDefinition = "TEXT")
        private String note;

        @Column(name = "linked_account", length = 100)
        private String linkedAccount;

        @Column(name = "goal_id")
        private UUID goalId;

        @Column(nullable = false, length = 10)
        private String status; // ACTIVE or DELETED

        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;
    }

}
