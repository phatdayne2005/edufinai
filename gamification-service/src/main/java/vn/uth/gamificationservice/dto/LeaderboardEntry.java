package vn.uth.gamificationservice.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    private UUID userId;
    private double score;
    private int top;

    public static LeaderboardEntry empty() {
        return new LeaderboardEntry(null, 0.0, -1);
    }
}
