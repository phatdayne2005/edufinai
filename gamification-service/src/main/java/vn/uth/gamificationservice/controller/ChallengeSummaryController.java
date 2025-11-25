package vn.uth.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.uth.gamificationservice.dto.ChallengeSummaryResponse;
import vn.uth.gamificationservice.dto.UserInfo;
import vn.uth.gamificationservice.service.ChallengeProgressService;
import vn.uth.gamificationservice.service.UserService;

@RestController
@RequestMapping("/api/challenges")
@Tag(name = "Challenge Summary Controller")
public class ChallengeSummaryController {

    private final ChallengeProgressService challengeProgressService;
    private final UserService userService;

    public ChallengeSummaryController(ChallengeProgressService challengeProgressService,
                                      UserService userService) {
        this.challengeProgressService = challengeProgressService;
        this.userService = userService;
    }

    @Operation(summary = "Get summarized challenge progress for current user")
    @GetMapping("/summary")
    public ResponseEntity<ChallengeSummaryResponse> getSummary() {
        UserInfo info = userService.getMyInfo();
        ChallengeSummaryResponse response = challengeProgressService.getSummary(info.getId());
        return ResponseEntity.ok(response);
    }
}

