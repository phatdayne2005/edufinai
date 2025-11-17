package vn.uth.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.uth.gamificationservice.dto.ApiResponse;
import vn.uth.gamificationservice.dto.UserBadgeResponse;
import vn.uth.gamificationservice.dto.UserInfo;
import vn.uth.gamificationservice.model.UserBadge;
import vn.uth.gamificationservice.service.BadgeService;
import vn.uth.gamificationservice.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/gamify")
@Tag(name = "Badge Controller")
public class BadgeController {

    private final BadgeService badgeService;
    private final UserService userService;

    public BadgeController(BadgeService badgeService, UserService userService) {
        this.badgeService = badgeService;
        this.userService = userService;
    }

    @Operation(summary = "Get badges of current user")
    @GetMapping("/badge/me")
    public ResponseEntity<ApiResponse<List<UserBadgeResponse>>> getMyBadges() {
        UserInfo info = userService.getMyInfo();
        List<UserBadge> badges = badgeService.getBadgesOfUser(info.getId());
        List<UserBadgeResponse> result = badges.stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result, "Badges retrieved successfully"));
    }

    private UserBadgeResponse toResponse(UserBadge userBadge) {
        UserBadgeResponse resp = new UserBadgeResponse();
        resp.setBadgeCode(userBadge.getBadge().getCode());
        resp.setBadgeName(userBadge.getBadge().getName());
        resp.setBadgeDescription(userBadge.getBadge().getDescription());
        resp.setBadgeType(userBadge.getBadge().getType());
        resp.setIconUrl(userBadge.getBadge().getIconUrl());
        resp.setCount(userBadge.getCount());
        resp.setFirstEarnedAt(userBadge.getFirstEarnedAt());
        resp.setLastEarnedAt(userBadge.getLastEarnedAt());
        resp.setSourceChallengeId(userBadge.getSourceChallengeId());
        return resp;
    }
}

