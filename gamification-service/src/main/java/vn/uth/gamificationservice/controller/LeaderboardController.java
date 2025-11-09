package vn.uth.gamificationservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.gamificationservice.dto.LeaderboardResponse;
import vn.uth.gamificationservice.dto.LeaderboardType;
import vn.uth.gamificationservice.service.LeaderboardService;


@RestController
@RequestMapping("/api/v1/gamify")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/leaderboard/{type}/{topNumber}")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(
            @PathVariable("type") String type,
            @PathVariable("topNumber") int topNumber) {
        try {
            LeaderboardType leaderboardType = LeaderboardType.valueOf(type.toUpperCase());
            LeaderboardResponse resp = this.leaderboardService.getTop(topNumber, leaderboardType);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new LeaderboardResponse(java.util.Collections.emptyList(), 
                            "Invalid leaderboard type. Valid types: DAILY, WEEKLY, MONTHLY, ALLTIME"));
        }
    }
}
