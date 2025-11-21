package vn.uth.gamificationservice.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_badges",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_badge", columnNames = {"user_id", "badge_id"})
)
public class UserBadge {
    @Id
    @GeneratedValue
    @Column(name = "user_badge_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "count", nullable = false)
    private Integer count;

    @Column(name = "first_earned_at", nullable = false)
    private LocalDateTime firstEarnedAt;

    @Column(name = "last_earned_at", nullable = false)
    private LocalDateTime lastEarnedAt;

    @Column(name = "source_challenge_id", columnDefinition = "BINARY(16)")
    private UUID sourceChallengeId;

    @PrePersist
    protected void onCreate() {
        if (count == null) {
            count = 0;
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

    public Badge getBadge() {
        return badge;
    }

    public void setBadge(Badge badge) {
        this.badge = badge;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public LocalDateTime getFirstEarnedAt() {
        return firstEarnedAt;
    }

    public void setFirstEarnedAt(LocalDateTime firstEarnedAt) {
        this.firstEarnedAt = firstEarnedAt;
    }

    public LocalDateTime getLastEarnedAt() {
        return lastEarnedAt;
    }

    public void setLastEarnedAt(LocalDateTime lastEarnedAt) {
        this.lastEarnedAt = lastEarnedAt;
    }

    public UUID getSourceChallengeId() {
        return sourceChallengeId;
    }

    public void setSourceChallengeId(UUID sourceChallengeId) {
        this.sourceChallengeId = sourceChallengeId;
    }
}

