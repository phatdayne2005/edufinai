package vn.uth.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.gamificationservice.dto.ChallengeResponse;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.service.ChallengeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gamify")
@Tag(name = "Challenge Controller")
public class ChallengeController {
    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @Operation(summary = "Get all challenge")
    @GetMapping("/challenge")
    public ResponseEntity<List<Challenge>> getChallenge() {
        List<Challenge> resp = challengeService.findAll();
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Create new challenge")
    @PostMapping("/challenge")
    public ResponseEntity<ChallengeResponse> save(@RequestBody Challenge newChallenge) {
        ChallengeResponse resp = this.challengeService.save(newChallenge);
        return ResponseEntity.ok(resp);
    }
}
