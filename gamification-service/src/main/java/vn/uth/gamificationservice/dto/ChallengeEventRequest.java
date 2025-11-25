package vn.uth.gamificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.UUID;

public class ChallengeEventRequest {
    @NotNull
    private UUID userId;

    @NotBlank
    private String eventType;

    @NotBlank
    private String action;

    private UUID lessonId;

    private String enrollId;

    private Integer score;

    private Integer accuracyPercent;

    private Integer totalQuestions;

    private Integer correctAnswers;

    private Integer amount; // default 1

    private ZonedDateTime occurredAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public ZonedDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(ZonedDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Integer getAccuracyPercent() {
        return accuracyPercent;
    }

    public void setAccuracyPercent(Integer accuracyPercent) {
        this.accuracyPercent = accuracyPercent;
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
}

