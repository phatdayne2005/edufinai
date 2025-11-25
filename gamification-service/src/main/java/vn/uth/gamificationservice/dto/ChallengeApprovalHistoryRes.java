package vn.uth.gamificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.uth.gamificationservice.model.ChallengeApprovalStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeApprovalHistoryRes {
    private UUID historyId;
    private ChallengeApprovalStatus status;
    private UUID reviewerId;
    private String note;
    private ZonedDateTime createdAt;
}

