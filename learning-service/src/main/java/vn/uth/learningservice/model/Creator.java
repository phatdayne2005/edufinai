package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class Creator {

    @Id
    @Column(name = "creator_id")
    @GeneratedValue
    private UUID id;

    // Cross-service reference to Lesson
    @OneToMany(mappedBy = "creator")
    private List<Lesson> lessons;

    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    // Bỏ
    // e.g. "Budgeting;Saving;Investing"
    @Size(max = 255)
    @Column(name = "expertise_areas", length = 255)
    private String expertiseAreas;

    // Bỏ
    @Size(max = 500)
    @Column(name = "bio", length = 500)
    private String bio;

    // Bỏ
    @Column(name = "total_lessons", nullable = false)
    private Integer totalLessons = 0;

    // Bỏ
    @DecimalMin("0.0") @DecimalMax("5.0")
    @Column(name = "rating", precision = 2)
    private Double rating = 0.0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
