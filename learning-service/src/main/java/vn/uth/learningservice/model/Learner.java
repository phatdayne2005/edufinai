package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class Learner {

    @Id
    @Column(name = "learner_id")
    @GeneratedValue
    private UUID id;

    @ManyToMany
    @JoinTable(name = "learner_lesson",
        joinColumns =  @JoinColumn(name = "learner_id"),
        inverseJoinColumns = @JoinColumn(name = "lesson_id"))
    private List<Lesson> lessons;

    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    @Min(10) @Max(100)
    @Column(name = "age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20)
    private Level level = Level.BEGINNER;

    @Size(max = 255)
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Size(max = 500)
    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "total_points")
    private Integer totalPoints = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Level { BEGINNER, INTERMEDIATE, ADVANCED }
}
