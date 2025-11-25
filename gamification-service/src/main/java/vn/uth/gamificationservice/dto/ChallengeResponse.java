package vn.uth.gamificationservice.dto;

import java.util.UUID;

public class ChallengeResponse {
    private UUID challengeId;
    private String status;

    public ChallengeResponse() {
    }

    public ChallengeResponse(UUID challengeId, String status) {
        this.challengeId = challengeId;
        this.status = status;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
