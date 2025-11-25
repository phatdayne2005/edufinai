package vn.uth.gamificationservice.dto;

import vn.uth.gamificationservice.model.Reward;

import java.util.List;
import java.util.UUID;

public class UserReward {
    private UUID userId;
    private Double totalScore;
    private List<Reward> rewardDetail;

    public UserReward() {
    }

    public UserReward(UUID userId, Double totalScore, List<Reward> rewardDetail) {
        this.userId = userId;
        this.totalScore = totalScore;
        this.rewardDetail = rewardDetail;
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

    public List<Reward> getRewardDetail() {
        return rewardDetail;
    }

    public void setRewardDetail(List<Reward> rewardDetail) {
        this.rewardDetail = rewardDetail;
    }
}
