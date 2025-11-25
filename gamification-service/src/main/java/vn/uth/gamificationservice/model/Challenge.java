package vn.uth.gamificationservice.model;

import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "challenges")
public class Challenge {
    @Id
    @GeneratedValue
    @Column(name = "challenge_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "title")
    @NotNull
    private String title;

    @Column(name = "description")
    @NotNull
    private String description;

    @Column(name = "type")
    @NotNull
    @Enumerated(EnumType.STRING)
    private ChallengeType type;

    @Column(name = "scope")
    @NotNull
    @Enumerated(EnumType.STRING)
    private ChallengeScope scope;

    @Column(name = "target_value")
    private Integer targetValue;

    @Column(name = "start_at")
    @NotNull
    private ZonedDateTime startAt;

    @Column(name = "end_at")
    @NotNull
    private ZonedDateTime endAt;

    @Column(name = "active")
    @NotNull
    private boolean active;

    @Column(name = "rule")
    @NotNull
    private String rule;

    @Column(name = "reward_score")
    private Integer rewardScore;

    @Column(name = "reward_badge_code")
    private String rewardBadgeCode;

    @Column(name = "max_progress_per_day")
    private Integer maxProgressPerDay;

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "approval_status")
    @Enumerated(EnumType.STRING)
    private ChallengeApprovalStatus approvalStatus;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ChallengeType getType() {
        return type;
    }

    public void setType(ChallengeType type) {
        this.type = type;
    }

    public ChallengeScope getScope() {
        return scope;
    }

    public void setScope(ChallengeScope scope) {
        this.scope = scope;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Integer targetValue) {
        this.targetValue = targetValue;
    }

    public ZonedDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(ZonedDateTime startAt) {
        this.startAt = startAt;
    }

    public ZonedDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(ZonedDateTime endAt) {
        this.endAt = endAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public Integer getRewardScore() {
        return rewardScore;
    }

    public void setRewardScore(Integer rewardScore) {
        this.rewardScore = rewardScore;
    }

    public String getRewardBadgeCode() {
        return rewardBadgeCode;
    }

    public void setRewardBadgeCode(String rewardBadgeCode) {
        this.rewardBadgeCode = rewardBadgeCode;
    }

    public Integer getMaxProgressPerDay() {
        return maxProgressPerDay;
    }

    public void setMaxProgressPerDay(Integer maxProgressPerDay) {
        this.maxProgressPerDay = maxProgressPerDay;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ChallengeApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ChallengeApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = ZonedDateTime.now(ZoneId.systemDefault());
        }
        if (this.approvalStatus == null) {
            this.approvalStatus = ChallengeApprovalStatus.PENDING;
        }
    }
}
