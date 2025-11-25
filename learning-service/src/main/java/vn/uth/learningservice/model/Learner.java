package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.*;

@Entity
@Table(name = "learner")
@Getter
@Setter
@NoArgsConstructor
public class Learner {

    @Id
    @Column(name = "learner_id", nullable = false)
    @NotNull
    private UUID id;

    @OneToMany(mappedBy = "learner")
    private List<Enrollment> enrollments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20, nullable = false)
    private Level level = Level.BEGINNER;

    // Total number of correct answers across all lessons
    @Column(name = "total_exp")
    private Long totalExp = 0L;

    // Current experience percentage (0-100%) towards next level
    @Min(0)
    @Max(100)
    @Column(name = "exp_percent")
    private Integer expPercent = 0;

    public enum Level {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}
