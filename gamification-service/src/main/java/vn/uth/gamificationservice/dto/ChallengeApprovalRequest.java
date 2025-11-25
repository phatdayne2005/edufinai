package vn.uth.gamificationservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vn.uth.gamificationservice.model.ChallengeApprovalStatus;

@Data
public class ChallengeApprovalRequest {
    @NotNull
    private ChallengeApprovalStatus status;

    private String note;
}

