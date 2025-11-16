package vn.uth.gamificationservice.dto;

import java.util.UUID;

public class RewardResponse {
    private UUID rewardId;
    private String status;  // ví dụ: "SUCCESS"

    // Constructor
    public RewardResponse(UUID rewardId, String status) {
        this.rewardId = rewardId;
        this.status = status;
    }

    public UUID getRewardId() {
        return rewardId;
    }

    public void setRewardId(UUID rewardId) {
        this.rewardId = rewardId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "RewardResponse{" +
                "rewardId=" + rewardId +
                ", status='" + status +
                '}';
    }
}
