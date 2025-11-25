package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "enrollment", uniqueConstraints = @UniqueConstraint(name = "uq_learner_lesson", columnNames = {
        "learner_id", "lesson_id" }))
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "enrollment_id", updatable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "learner_id", nullable = false)
    private Learner learner;

    @ManyToOne
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.IN_PROGRESS; // IN_PROGRESS, COMPLETED

    @Min(0)
    @Max(100)
    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent = 0;

    @Min(0)
    @Column(name = "score")
    private Integer score;

    @Min(0)
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "earned_exp")
    private Long earnedExp = 0L;

    @Column(name = "correct_answers")
    private Integer correctAnswers = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    public enum Status {
        IN_PROGRESS, COMPLETED
    }
}
