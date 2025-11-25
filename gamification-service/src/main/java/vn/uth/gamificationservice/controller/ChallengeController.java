package vn.uth.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import vn.uth.gamificationservice.dto.*;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.model.ChallengeApprovalStatus;
import vn.uth.gamificationservice.service.ChallengeService;
import vn.uth.gamificationservice.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gamify")
@Tag(name = "Challenge Controller")
public class ChallengeController {
    private final ChallengeService challengeService;
    private final UserService userService;

    public ChallengeController(ChallengeService challengeService, UserService userService) {
        this.challengeService = challengeService;
        this.userService = userService;
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

//    @Operation(summary = "Update a challenge")
//    @PutMapping("/challenge")
//    public ResponseEntity<ChallengeResponse> update(@RequestBody Challenge challenge) {
//        return
//    }

    @Operation(summary = "Delete a challenge")
    @DeleteMapping("/challenge/{challengeId}")
    public ResponseEntity<SimpleResponse> delete(@PathVariable UUID challengeId) {
        this.challengeService.delete(challengeId);
        SimpleResponse resp = new SimpleResponse("SUCCESS");
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Get challenges filtered by approval status")
    @GetMapping("/challenge/status/{status}")
    public ResponseEntity<List<Challenge>> getChallengeByStatus(@PathVariable("status") ChallengeApprovalStatus status) {
        return ResponseEntity.ok(challengeService.findByApprovalStatus(status));
    }

    @Operation(summary = "Approve or reject a challenge")
    @PatchMapping("/challenge/{challengeId}/approval")
    public ResponseEntity<ChallengeResponse> updateApproval(
            @PathVariable UUID challengeId,
            @Valid @RequestBody ChallengeApprovalRequest request) {
        var userInfo = userService.getMyInfo();
        this.challengeService.updateApprovalStatus(challengeId, request.getStatus(), userInfo.getId(), request.getNote());
        ChallengeResponse resp = new ChallengeResponse(challengeId, request.getStatus().name());
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Resubmit challenge for approval")
    @PostMapping("/challenge/{challengeId}/resubmit")
    public ResponseEntity<ChallengeResponse> resubmit(@PathVariable UUID challengeId) {
        var userInfo = userService.getMyInfo();
        this.challengeService.resubmit(challengeId, userInfo.getId());
        return ResponseEntity.ok(new ChallengeResponse(challengeId, "RE_SUBMITTED"));
    }

    @Operation(summary = "Get approval history for challenge")
    @GetMapping("/challenge/{challengeId}/approval-history")
    public ResponseEntity<List<ChallengeApprovalHistoryRes>> history(@PathVariable UUID challengeId) {
        List<ChallengeApprovalHistoryRes> history = challengeService.getApprovalHistory(challengeId).stream()
                .map(h -> new ChallengeApprovalHistoryRes(
                        h.getId(),
                        h.getStatus(),
                        h.getReviewerId(),
                        h.getNote(),
                        h.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/me")
    public Object me(JwtAuthenticationToken token) {
        String sub = token.getToken().getClaim("sub");
        String scope = token.getToken().getClaim("scope");
        return Map.of(
                "sub", sub,
                "scope", scope
        );
    }
}
