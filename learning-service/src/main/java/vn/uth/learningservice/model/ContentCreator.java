package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

public class ContentCreator {
    @Id
    @Column(name = "creator_id", columnDefinition = "CHAR(36)")
    private UUID id;

    // Cross-service reference to User Service
    @NotNull
    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false, unique = true)
    private UUID userId;

    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    // e.g. "Budgeting;Saving;Investing"
    @Size(max = 255)
    @Column(name = "expertise_areas", length = 255)
    private String expertiseAreas;

    @Size(max = 500)
    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "total_lessons", nullable = false)
    private Integer totalLessons = 0;

    @DecimalMin("0.0") @DecimalMax("5.0")
    @Column(name = "rating", precision = 2)
    private Double rating = 0.0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
