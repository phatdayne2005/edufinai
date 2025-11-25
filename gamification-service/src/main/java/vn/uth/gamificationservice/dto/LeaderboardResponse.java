package vn.uth.gamificationservice.dto;

import java.util.List;

public class LeaderboardResponse {
    private List<LeaderboardEntry> result;
    private String status;

    public LeaderboardResponse() {
    }

    public LeaderboardResponse(List<LeaderboardEntry> result, String status) {
        this.result = result;
        this.status = status;
    }

    public List<LeaderboardEntry> getResult() {
        return result;
    }

    public void setResult(List<LeaderboardEntry> result) {
        this.result = result;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "LeaderboardResponse{" +
                "result=" + result +
                ", status='" + status + '\'' +
                '}';
    }
}
