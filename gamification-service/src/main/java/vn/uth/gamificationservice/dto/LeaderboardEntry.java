package vn.uth.gamificationservice.dto;

import java.util.UUID;

public class LeaderboardEntry {
    private UUID userId;
    private double score;
    private int top;
    private String name;
    private String username;

    public LeaderboardEntry() {
    }

    public LeaderboardEntry(UUID userId, double score, int top, String name, String username) {
        this.userId = userId;
        this.score = score;
        this.top = top;
        this.name = name;
        this.username = username;
    }

    public static LeaderboardEntry empty() {
        return new LeaderboardEntry(null, 0.0, -1, null, null);
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "LeaderboardEntry{" +
                "userId=" + userId +
                ", score=" + score +
                ", top=" + top +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
