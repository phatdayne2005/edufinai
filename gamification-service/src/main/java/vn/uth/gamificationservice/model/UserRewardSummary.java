package vn.uth.gamificationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_rewards")
public class UserRewardSummary {
    @Id
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "total_score")
    private Double totalScore;

    public UserRewardSummary() {
    }

    public UserRewardSummary(UUID userId, Double totalScore) {
        this.userId = userId;
        this.totalScore = totalScore;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }
}
