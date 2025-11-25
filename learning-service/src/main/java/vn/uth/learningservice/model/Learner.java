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

    @Column(name = "total_exp")
    private Long totalExp = 0L;

    public enum Level {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}
