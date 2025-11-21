package vn.uth.gamificationservice.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_challenge_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_challenge", columnNames = {"user_id", "challenge_id"})
)
public class UserChallengeProgress {
    @Id
    @GeneratedValue
    @Column(name = "progress_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "current_progress", nullable = false)
    private Integer currentProgress;

    @Column(name = "target_progress", nullable = false)
    private Integer targetProgress;

    @Column(name = "completed", nullable = false)
    private Boolean completed;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    @Column(name = "last_progress_date")
    private LocalDate lastProgressDate;

    @Column(name = "progress_count_today", nullable = false)
    private Integer progressCountToday;

    @Column(name = "started_at", nullable = false)
    private ZonedDateTime startedAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        ZonedDateTime now = ZonedDateTime.now();
        if (startedAt == null) {
            startedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (currentProgress == null) {
            currentProgress = 0;
        }
        if (progressCountToday == null) {
            progressCountToday = 0;
        }
        if (completed == null) {
            completed = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    // getters/setters

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

    public Challenge getChallenge() {
        return challenge;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }

    public Integer getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(Integer currentProgress) {
        this.currentProgress = currentProgress;
    }

    public Integer getTargetProgress() {
        return targetProgress;
    }

    public void setTargetProgress(Integer targetProgress) {
        this.targetProgress = targetProgress;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(ZonedDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDate getLastProgressDate() {
        return lastProgressDate;
    }

    public void setLastProgressDate(LocalDate lastProgressDate) {
        this.lastProgressDate = lastProgressDate;
    }

    public Integer getProgressCountToday() {
        return progressCountToday;
    }

    public void setProgressCountToday(Integer progressCountToday) {
        this.progressCountToday = progressCountToday;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

