package vn.uth.gamificationservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import vn.uth.gamificationservice.model.RewardSourceType;

import java.util.UUID;

public class RewardRequest {
    @NotNull(message = "User ID không được để trống")
    private UUID userId;

    private Integer score;

    private RewardSourceType sourceType;

    private UUID lessonId;

    private String enrollId;

    private UUID challengeId;

    private String badge;

    private String reason;

    @Min(value = 0, message = "totalQuestions không hợp lệ")
    private Integer totalQuestions;

    @Min(value = 0, message = "correctAnswers không hợp lệ")
    private Integer correctAnswers;

    public @NotNull UUID getUserId() {
        return userId;
    }

    public void setUserId(@NotNull UUID userId) {
        this.userId = userId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
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

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(Integer correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    @Override
    public String toString() {
        return "RewardRequest{" +
                "userId=" + userId +
                ", score=" + score +
                ", sourceType=" + sourceType +
                ", lessonId=" + lessonId +
                ", enrollId='" + enrollId + '\'' +
                ", challengeId=" + challengeId +
                ", badge='" + badge + '\'' +
                ", reason='" + reason + '\'' +
                ", totalQuestions=" + totalQuestions +
                ", correctAnswers=" + correctAnswers +
                '}';
    }
}
