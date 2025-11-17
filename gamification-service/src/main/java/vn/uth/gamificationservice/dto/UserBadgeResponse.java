package vn.uth.gamificationservice.dto;

import vn.uth.gamificationservice.model.BadgeType;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserBadgeResponse {
    private String badgeCode;
    private String badgeName;
    private BadgeType badgeType;
    private String badgeDescription;
    private String iconUrl;
    private Integer count;
    private LocalDateTime firstEarnedAt;
    private LocalDateTime lastEarnedAt;
    private UUID sourceChallengeId;

    public String getBadgeCode() {
        return badgeCode;
    }

    public void setBadgeCode(String badgeCode) {
        this.badgeCode = badgeCode;
    }

    public String getBadgeName() {
        return badgeName;
    }

    public void setBadgeName(String badgeName) {
        this.badgeName = badgeName;
    }

    public BadgeType getBadgeType() {
        return badgeType;
    }

    public void setBadgeType(BadgeType badgeType) {
        this.badgeType = badgeType;
    }

    public String getBadgeDescription() {
        return badgeDescription;
    }

    public void setBadgeDescription(String badgeDescription) {
        this.badgeDescription = badgeDescription;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public LocalDateTime getFirstEarnedAt() {
        return firstEarnedAt;
    }

    public void setFirstEarnedAt(LocalDateTime firstEarnedAt) {
        this.firstEarnedAt = firstEarnedAt;
    }

    public LocalDateTime getLastEarnedAt() {
        return lastEarnedAt;
    }

    public void setLastEarnedAt(LocalDateTime lastEarnedAt) {
        this.lastEarnedAt = lastEarnedAt;
    }

    public UUID getSourceChallengeId() {
        return sourceChallengeId;
    }

    public void setSourceChallengeId(UUID sourceChallengeId) {
        this.sourceChallengeId = sourceChallengeId;
    }
}

