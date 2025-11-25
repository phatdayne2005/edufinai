package vn.uth.gamificationservice.dto;

import jakarta.validation.constraints.NotNull;
import vn.uth.gamificationservice.model.ChallengeApprovalStatus;

public class ChallengeApprovalRequest {
    @NotNull
    private ChallengeApprovalStatus status;

    private String note;

    public ChallengeApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ChallengeApprovalStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

