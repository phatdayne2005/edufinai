package vn.uth.gamificationservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChallengeRule {
    private String eventType;
    private String action;
    private Integer count;
    private Integer minScore;
    private Integer maxScore;
    private Integer maxProgressPerDay;
    private Integer minAccuracy;
    private Integer maxAccuracy;

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

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getMinScore() {
        return minScore;
    }

    public void setMinScore(Integer minScore) {
        this.minScore = minScore;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public Integer getMaxProgressPerDay() {
        return maxProgressPerDay;
    }

    public void setMaxProgressPerDay(Integer maxProgressPerDay) {
        this.maxProgressPerDay = maxProgressPerDay;
    }

    public Integer getMinAccuracy() {
        return minAccuracy;
    }

    public void setMinAccuracy(Integer minAccuracy) {
        this.minAccuracy = minAccuracy;
    }

    public Integer getMaxAccuracy() {
        return maxAccuracy;
    }

    public void setMaxAccuracy(Integer maxAccuracy) {
        this.maxAccuracy = maxAccuracy;
    }
}

