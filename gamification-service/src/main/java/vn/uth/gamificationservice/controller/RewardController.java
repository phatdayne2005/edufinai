package vn.uth.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.gamificationservice.dto.RewardRequest;
import vn.uth.gamificationservice.dto.RewardResponse;
import vn.uth.gamificationservice.dto.UserReward;
import vn.uth.gamificationservice.service.RewardService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gamify")
@Tag(name = "Reward Controller")
public class RewardController {
    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;

    }

    @Operation(summary = "Add user reward")
    @PostMapping("/reward")
    public ResponseEntity<RewardResponse> addReward(@Valid @RequestBody RewardRequest req) {
        RewardResponse resp = rewardService.addReward(req);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Check user reward")
    @GetMapping("/reward")
    public ResponseEntity<UserReward> getRewards() {
        UserReward resp = rewardService.getUserReward();
        return ResponseEntity.ok(resp);
    }
}
