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
    @GeneratedValue(strategy =  GenerationType.UUID)
    private UUID id;

    // Cross-service reference to Lesson
    @OneToMany(mappedBy = "creator")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<Lesson> lessons;

    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
