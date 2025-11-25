package vn.uth.gamificationservice.dto;

import java.util.UUID;

public class ChallengeSummaryItem {
    private UUID challengeId;
    private String content;
    private double progress;

    public ChallengeSummaryItem() {
    }

    public ChallengeSummaryItem(UUID challengeId, String content, double progress) {
        this.challengeId = challengeId;
        this.content = content;
        this.progress = progress;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public String getContent() {
        return content;
    }

    public double getProgress() {
        return progress;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }
}

