package vn.uth.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.gamificationservice.dto.ApiResponse;
import vn.uth.gamificationservice.dto.LeaderboardEntry;
import vn.uth.gamificationservice.dto.LeaderboardResponse;
import vn.uth.gamificationservice.dto.LeaderboardType;
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

    @Operation(summary = "Check my top")
    @GetMapping("leaderboard/{type}/me")
    public ResponseEntity<ApiResponse<LeaderboardEntry>> getMyLeaderboard(@PathVariable("type") String type){
        LeaderboardType leaderboardType = LeaderboardType.valueOf(type.toUpperCase());
        ApiResponse<LeaderboardEntry> resp = this.leaderboardService.getCurrentUserTop(leaderboardType);
        return ResponseEntity.ok(resp);
    }
}
