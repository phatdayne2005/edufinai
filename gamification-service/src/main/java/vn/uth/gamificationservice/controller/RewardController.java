package vn.uth.gamificationservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.gamificationservice.dto.RewardRequest;
import vn.uth.gamificationservice.dto.RewardResponse;
import vn.uth.gamificationservice.service.RewardService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gamify")
public class RewardController {
    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;

    }

    @PostMapping("/reward")
    public ResponseEntity<RewardResponse> addReward(@Valid @RequestBody RewardRequest req) {
        RewardResponse resp = rewardService.addReward(req);
        return ResponseEntity.ok(resp);
    }
}
