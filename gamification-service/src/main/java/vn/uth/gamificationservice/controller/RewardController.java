package vn.uth.gamificationservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.uth.gamificationservice.dto.RewardRequest;
import vn.uth.gamificationservice.dto.RewardResponse;
import vn.uth.gamificationservice.service.RewardService;

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
        System.out.println(req.toString());
        System.out.println(resp.toString());
        return ResponseEntity.ok(resp);
    }
}
