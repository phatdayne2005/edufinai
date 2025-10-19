package vn.uth.gamificationservice.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.uth.gamificationservice.dto.LeaderboardResponse;
import vn.uth.gamificationservice.dto.RewardResponse;
import vn.uth.gamificationservice.service.LeaderboardService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gamify")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(RedisTemplate redisTemplate) {
        this.leaderboardService = new LeaderboardService(redisTemplate);
    }

    @GetMapping("/leaderboard/{topNumber}")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(@PathVariable("topNumber") int topNumber) {
        LeaderboardResponse resp = this.leaderboardService.getTop(topNumber);
        return ResponseEntity.ok(resp);
    }
}
