package vn.uth.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.gamificationservice.dto.ChallengeResponse;
import vn.uth.gamificationservice.dto.SimpleResponse;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.service.ChallengeService;

import java.util.List;
import java.util.UUID;

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
        this.challengeService.save(newChallenge);
        ChallengeResponse resp = new ChallengeResponse(newChallenge.getId(), "SUCCESS");
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Delete a challenge")
    @DeleteMapping("/challenge/{challengeId}")
    public ResponseEntity<SimpleResponse> delete(@PathVariable UUID challengeId) {
        this.challengeService.delete(challengeId);
        SimpleResponse resp = new SimpleResponse("SUCCESS");
        return ResponseEntity.ok(resp);
    }
}
