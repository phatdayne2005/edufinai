package vn.uth.gamificationservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class RewardRequest {
    @NotNull(message = "User ID không được để trống")
    private UUID userId;

    @NotBlank(message = "Badge không được để trống")
    private String badge;

    @NotNull(message = "Score không được để trống")
    private Integer score;

    private String reason;

    public @NotNull UUID getUserId() {
        return userId;
    }

    public void setUserId(@NotNull UUID userId) {
        this.userId = userId;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public @NotNull Integer getScore() {
        return score;
    }

    public void setScore(@NotNull Integer score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "RewardRequest{" +
                "userId=" + userId +
                ", badge='" + badge + '\'' +
                ", score=" + score +
                ", reason='" + reason + '\'' +
                '}';
    }
}
