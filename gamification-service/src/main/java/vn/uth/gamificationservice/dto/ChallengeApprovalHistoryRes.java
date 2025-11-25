package vn.uth.gamificationservice.dto;

import vn.uth.gamificationservice.model.ChallengeApprovalStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

public class ChallengeApprovalHistoryRes {
    private UUID historyId;
    private ChallengeApprovalStatus status;
    private UUID reviewerId;
    private String note;
    private ZonedDateTime createdAt;

    public ChallengeApprovalHistoryRes() {
    }

    public ChallengeApprovalHistoryRes(UUID historyId,
                                       ChallengeApprovalStatus status,
                                       UUID reviewerId,
                                       String note,
                                       ZonedDateTime createdAt) {
        this.historyId = historyId;
        this.status = status;
        this.reviewerId = reviewerId;
        this.note = note;
        this.createdAt = createdAt;
    }

    public UUID getHistoryId() {
        return historyId;
    }

    public ChallengeApprovalStatus getStatus() {
        return status;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public String getNote() {
        return note;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
}

