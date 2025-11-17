package vn.uth.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.gamificationservice.dto.ApiResponse;
import vn.uth.gamificationservice.dto.ChallengeEventRequest;
import vn.uth.gamificationservice.dto.ChallengeProgressResponse;
import vn.uth.gamificationservice.dto.UserInfo;
import vn.uth.gamificationservice.model.UserChallengeProgress;
import vn.uth.gamificationservice.service.ChallengeProgressService;
import vn.uth.gamificationservice.service.UserService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/gamify")
@Tag(name = "Challenge Progress Controller")
public class ChallengeProgressController {

    private final ChallengeProgressService challengeProgressService;
    private final UserService userService;

    public ChallengeProgressController(ChallengeProgressService challengeProgressService, UserService userService) {
        this.challengeProgressService = challengeProgressService;
        this.userService = userService;
    }

    @Operation(summary = "Publish challenge event (service-to-service)")
    @PostMapping("/challenge/event")
    public ResponseEntity<ApiResponse<Void>> publishEvent(@Valid @RequestBody ChallengeEventRequest request) {
        challengeProgressService.processEvent(request);
        return ResponseEntity.ok(ApiResponse.success("Event processed"));
    }

    @Operation(summary = "Get progress of challenge for current user")
    @GetMapping("/challenge/{challengeId}/progress")
    public ResponseEntity<ApiResponse<ChallengeProgressResponse>> getProgress(@PathVariable UUID challengeId) {
        UserInfo info = userService.getMyInfo();
        UserChallengeProgress progress = challengeProgressService.getProgress(info.getId(), challengeId);
        if (progress == null) {
            return ResponseEntity.ok(ApiResponse.empty("No progress found"));
        }
        return ResponseEntity.ok(ApiResponse.success(toResponse(progress), "Progress retrieved"));
    }

    @Operation(summary = "Get active challenges of current user")
    @GetMapping("/challenge/me/active")
    public ResponseEntity<ApiResponse<List<ChallengeProgressResponse>>> getActiveChallenges() {
        UserInfo info = userService.getMyInfo();
        List<ChallengeProgressResponse> responses = challengeProgressService.getActiveProgress(info.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses, "Active challenges retrieved"));
    }

    @Operation(summary = "Get completed challenges of current user")
    @GetMapping("/challenge/me/completed")
    public ResponseEntity<ApiResponse<List<ChallengeProgressResponse>>> getCompletedChallenges() {
        UserInfo info = userService.getMyInfo();
        List<ChallengeProgressResponse> responses = challengeProgressService.getCompletedProgress(info.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses, "Completed challenges retrieved"));
    }

    private ChallengeProgressResponse toResponse(UserChallengeProgress progress) {
        ChallengeProgressResponse resp = new ChallengeProgressResponse();
        resp.setChallengeId(progress.getChallenge().getId());
        resp.setTitle(progress.getChallenge().getTitle());
        resp.setCurrentProgress(progress.getCurrentProgress());
        resp.setTargetProgress(progress.getTargetProgress());
        resp.setCompleted(Boolean.TRUE.equals(progress.getCompleted()));
        resp.setCompletedAt(progress.getCompletedAt());
        return resp;
    }
}

