package vn.uth.gamificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeSummaryItem {
    private UUID challengeId;
    private String content;
    private double progress;
}

