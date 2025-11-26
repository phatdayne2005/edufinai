package vn.uth.learningservice.dto.request;

import java.util.UUID;

public class RewardRequest {
    private UUID userId;
    private String sourceType; // "QUIZ", "CHALLENGE", "MANUAL"
    private UUID lessonId;
    private String enrollId;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer score;
    private String badge;
    private String reason;
    private UUID challengeId;

    public RewardRequest() {
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
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

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
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

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }
}

