package vn.uth.financeservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "category", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "name"})
})
@Getter
@Setter
public class Category {
    @Id
    @Column(name = "category_id", columnDefinition = "BINARY(16)")
    private UUID categoryId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryType type; // INCOME, EXPENSE, BOTH

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}


