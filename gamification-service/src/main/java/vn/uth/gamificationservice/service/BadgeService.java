package vn.uth.gamificationservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.gamificationservice.dto.BadgeCreateRequest;
import vn.uth.gamificationservice.model.Badge;
import vn.uth.gamificationservice.model.UserBadge;
import vn.uth.gamificationservice.repository.BadgeRepository;
import vn.uth.gamificationservice.repository.UserBadgeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    public BadgeService(BadgeRepository badgeRepository, UserBadgeRepository userBadgeRepository) {
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
    }

    @Transactional
    public UserBadge awardBadge(UUID userId, String badgeCode, UUID sourceChallengeId) {
        if (badgeCode == null || badgeCode.isBlank()) {
            return null;
        }
        Badge badge = badgeRepository.findByCode(badgeCode)
                .orElseThrow(() -> new IllegalArgumentException("Badge code not found: " + badgeCode));

        UserBadge userBadge = userBadgeRepository.findByUserIdAndBadge_Id(userId, badge.getId())
                .orElseGet(() -> createUserBadge(userId, badge));

        LocalDateTime now = LocalDateTime.now();
        if (userBadge.getCount() == 0) {
            userBadge.setFirstEarnedAt(now);
        }
        userBadge.setCount(userBadge.getCount() + 1);
        userBadge.setLastEarnedAt(now);
        userBadge.setSourceChallengeId(sourceChallengeId);

        return userBadgeRepository.save(userBadge);
    }

    public List<UserBadge> getBadgesOfUser(UUID userId) {
        return userBadgeRepository.findByUserId(userId);
    }

    public List<Badge> getAllBadges() {
        return badgeRepository.findAll();
    }

    @Transactional
    public Badge createBadge(BadgeCreateRequest request) {
        if (badgeRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Badge code already exists: " + request.getCode());
        }
        Badge badge = new Badge();
        badge.setCode(request.getCode());
        badge.setName(request.getName());
        badge.setDescription(request.getDescription());
        badge.setType(request.getType());
        badge.setIconUrl(request.getIconUrl());
        return badgeRepository.save(badge);
    }

    private UserBadge createUserBadge(UUID userId, Badge badge) {
        UserBadge userBadge = new UserBadge();
        userBadge.setUserId(userId);
        userBadge.setBadge(badge);
        userBadge.setCount(0);
        userBadge.setFirstEarnedAt(LocalDateTime.now());
        userBadge.setLastEarnedAt(LocalDateTime.now());
        return userBadge;
    }
}

