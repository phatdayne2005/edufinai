package vn.uth.gamificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.gamificationservice.dto.ChallengeEventRequest;
import vn.uth.gamificationservice.dto.ChallengeRule;
import vn.uth.gamificationservice.dto.RewardRequest;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.model.RewardSourceType;
import vn.uth.gamificationservice.model.UserChallengeProgress;
import vn.uth.gamificationservice.repository.ChallengeRepository;
import vn.uth.gamificationservice.repository.UserChallengeProgressRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChallengeProgressService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeProgressService.class);

    private final ChallengeRepository challengeRepository;
    private final UserChallengeProgressRepository progressRepository;
    private final ChallengeRuleEvaluator ruleEvaluator;
    private final RewardService rewardService;
    private final BadgeService badgeService;

    public ChallengeProgressService(ChallengeRepository challengeRepository,
                                    UserChallengeProgressRepository progressRepository,
                                    ChallengeRuleEvaluator ruleEvaluator,
                                    @Lazy RewardService rewardService,
                                    BadgeService badgeService) {
        this.challengeRepository = challengeRepository;
        this.progressRepository = progressRepository;
        this.ruleEvaluator = ruleEvaluator;
        this.rewardService = rewardService;
        this.badgeService = badgeService;
    }

    @Transactional
    public void processEvent(ChallengeEventRequest request) {
        ZonedDateTime now = request.getOccurredAt() != null ? request.getOccurredAt() : ZonedDateTime.now();
        List<Challenge> activeChallenges = challengeRepository
                .findByActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqual(now, now);

        for (Challenge challenge : activeChallenges) {
            handleChallenge(challenge, request);
        }
    }

    private void handleChallenge(Challenge challenge, ChallengeEventRequest event) {
        ChallengeRule rule = ruleEvaluator.parse(challenge.getRule());
        if (!ruleEvaluator.matches(rule, event)) {
            return;
        }

        UserChallengeProgress progress = progressRepository
                .findByUserIdAndChallenge_Id(event.getUserId(), challenge.getId())
                .orElseGet(() -> createProgress(event.getUserId(), challenge, rule));

        if (Boolean.TRUE.equals(progress.getCompleted())) {
            return;
        }

        if (!canIncrease(progress, challenge, rule)) {
            return;
        }

        int increment = event.getAmount() != null ? event.getAmount() : 1;
        progress.setCurrentProgress(progress.getCurrentProgress() + increment);

        LocalDate today = LocalDate.now();
        if (progress.getLastProgressDate() == null || !progress.getLastProgressDate().equals(today)) {
            progress.setProgressCountToday(0);
            progress.setLastProgressDate(today);
        }
        progress.setProgressCountToday(progress.getProgressCountToday() + increment);

        if (progress.getCurrentProgress() >= progress.getTargetProgress()) {
            completeChallenge(progress, challenge, event.getUserId());
        }

        progressRepository.save(progress);
    }

    private UserChallengeProgress createProgress(UUID userId, Challenge challenge, ChallengeRule rule) {
        UserChallengeProgress progress = new UserChallengeProgress();
        progress.setUserId(userId);
        progress.setChallenge(challenge);
        progress.setCurrentProgress(0);
        progress.setTargetProgress(ruleEvaluator.resolveTarget(rule, challenge.getTargetValue()));
        progress.setCompleted(false);
        progress.setProgressCountToday(0);
        progress.setStartedAt(ZonedDateTime.now());
        progress.setUpdatedAt(ZonedDateTime.now());
        return progress;
    }

    private boolean canIncrease(UserChallengeProgress progress, Challenge challenge, ChallengeRule rule) {
        Integer maxPerDay = challenge.getMaxProgressPerDay();
        if ((maxPerDay == null || maxPerDay <= 0) && rule != null) {
            maxPerDay = rule.getMaxProgressPerDay();
        }
        if (maxPerDay == null || maxPerDay <= 0) {
            return true;
        }
        LocalDate today = LocalDate.now();
        if (progress.getLastProgressDate() == null || !progress.getLastProgressDate().equals(today)) {
            progress.setProgressCountToday(0);
            progress.setLastProgressDate(today);
        }
        return progress.getProgressCountToday() < maxPerDay;
    }

    private void completeChallenge(UserChallengeProgress progress, Challenge challenge, UUID userId) {
        progress.setCompleted(true);
        progress.setCompletedAt(ZonedDateTime.now());

        if (challenge.getRewardScore() != null && challenge.getRewardScore() > 0) {
            RewardRequest rewardRequest = new RewardRequest();
            rewardRequest.setUserId(userId);
            rewardRequest.setScore(challenge.getRewardScore());
            rewardRequest.setSourceType(RewardSourceType.CHALLENGE);
            rewardRequest.setChallengeId(challenge.getId());
            rewardRequest.setReason("Challenge completed: " + challenge.getTitle());
            try {
                rewardService.addReward(rewardRequest);
            } catch (Exception ex) {
                log.error("Failed to grant reward for challenge completion", ex);
            }
        }

        try {
            badgeService.awardBadge(userId, challenge.getRewardBadgeCode(), challenge.getId());
        } catch (Exception ex) {
            log.error("Failed to award badge for challenge completion", ex);
        }
    }

    public List<UserChallengeProgress> getActiveProgress(UUID userId) {
        return progressRepository.findByUserIdAndCompletedFalse(userId);
    }

    public List<UserChallengeProgress> getCompletedProgress(UUID userId) {
        return progressRepository.findByUserIdAndCompletedTrue(userId);
    }

    public UserChallengeProgress getProgress(UUID userId, UUID challengeId) {
        return progressRepository.findByUserIdAndChallenge_Id(userId, challengeId).orElse(null);
    }
}

