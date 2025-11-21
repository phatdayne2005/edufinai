package vn.uth.gamificationservice.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rewards")
public class Reward {
    @Id
    @GeneratedValue
    @Column(name = "reward_id", columnDefinition = "BINARY(16)")
    private UUID rewardId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "badge")
    private String badge;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private RewardSourceType sourceType;

    @Column(name = "lesson_id", columnDefinition = "BINARY(16)")
    private UUID lessonId;

    @Column(name = "enroll_id", length = 64)
    private String enrollId;

    @Column(name = "challenge_id", columnDefinition = "BINARY(16)")
    private UUID challengeId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Reward() {
    }

    public UUID getRewardId() {
        return rewardId;
    }

    public void setRewardId(UUID rewardId) {
        this.rewardId = rewardId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RewardSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(RewardSourceType sourceType) {
        this.sourceType = sourceType;
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

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Reward{" +
                "rewardId=" + rewardId +
                ", userId=" + userId +
                ", badge='" + badge + '\'' +
                ", score=" + score +
                ", reason='" + reason + '\'' +
                ", sourceType=" + sourceType +
                ", lessonId=" + lessonId +
                ", enrollId='" + enrollId + '\'' +
                ", challengeId=" + challengeId +
                ", createdAt=" + createdAt +
                '}';
    }
}
