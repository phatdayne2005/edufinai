package vn.uth.gamificationservice.dto;

import java.util.List;

public class ChallengeSummaryResponse {
    private List<ChallengeSummaryItem> challenges;
    private long totalCount;

    public ChallengeSummaryResponse() {
    }

    public ChallengeSummaryResponse(List<ChallengeSummaryItem> challenges, long totalCount) {
        this.challenges = challenges;
        this.totalCount = totalCount;
    }

    public List<ChallengeSummaryItem> getChallenges() {
        return challenges;
    }

    public void setChallenges(List<ChallengeSummaryItem> challenges) {
        this.challenges = challenges;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
}

