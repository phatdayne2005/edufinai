package vn.uth.gamificationservice.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "lesson_attempt_history",
        uniqueConstraints = @UniqueConstraint(name = "uk_enroll_id", columnNames = {"enroll_id"})
)
public class LessonAttemptHistory {
    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "lesson_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID lessonId;

    @Column(name = "enroll_id", nullable = false, length = 64)
    private String enrollId;

    @Column(name = "raw_score", nullable = false)
    private Integer rawScore;

    @Column(name = "delta_score", nullable = false)
    private Integer deltaScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public LessonAttemptHistory() {
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public void setLessonId(UUID lessonId) {
        this.lessonId = lessonId;
    }

    public String getEnrollId() {
        return enrollId;
    }

    public void setEnrollId(String enrollId) {
        this.enrollId = enrollId;
    }

    public Integer getRawScore() {
        return rawScore;
    }

    public void setRawScore(Integer rawScore) {
        this.rawScore = rawScore;
    }

    public Integer getDeltaScore() {
        return deltaScore;
    }

    public void setDeltaScore(Integer deltaScore) {
        this.deltaScore = deltaScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

