package vn.uth.gamificationservice.dto;

public class LeaderboardEntry {
    private String userId;
    private double score;

    public LeaderboardEntry(String userId, double score) {
        this.userId = userId;
        this.score = score;
    }

    public String getUserId() {
        return userId;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "LeaderboardEntry{" +
                "userId='" + userId + '\'' +
                ", score=" + score +
                '}';
    }
}
