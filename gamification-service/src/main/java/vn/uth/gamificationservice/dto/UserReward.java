package vn.uth.gamificationservice.dto;

import java.util.UUID;

public class UserReward {
    private UUID userId;
    private Double reward;

    public UserReward(UUID userId, Double reward) {
        this.userId = userId;
        this.reward = reward;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Double getReward() {
        return reward;
    }

    public void setReward(Double reward) {
        this.reward = reward;
    }

    @Override
    public String toString() {
        return "UserReward{" +
                "userId=" + userId +
                ", reward=" + reward +
                '}';
    }
}
