package vn.uth.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.gamificationservice.dto.LeaderboardResponse;
import vn.uth.gamificationservice.service.LeaderboardService;


@RestController
@RequestMapping("/api/v1/gamify")
@Tag(name = "Leaderboard Controller")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @Operation(summary = "Check top number")
    @GetMapping("/leaderboard/{topNumber}")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(@PathVariable("topNumber") int topNumber) {
        LeaderboardResponse resp = this.leaderboardService.getTop(topNumber);
        return ResponseEntity.ok(resp);
    }
}
