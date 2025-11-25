package vn.uth.gamificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeSummaryResponse {
    private List<ChallengeSummaryItem> challenges;
    private long totalCount;
}

