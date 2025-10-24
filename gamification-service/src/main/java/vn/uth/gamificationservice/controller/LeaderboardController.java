package vn.uth.gamificationservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.gamificationservice.dto.LeaderboardResponse;
import vn.uth.gamificationservice.service.LeaderboardService;


@RestController
@RequestMapping("/api/v1/gamify")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/leaderboard/{topNumber}")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(@PathVariable("topNumber") int topNumber) {
        LeaderboardResponse resp = this.leaderboardService.getTop(topNumber);
        return ResponseEntity.ok(resp);
    }
}
