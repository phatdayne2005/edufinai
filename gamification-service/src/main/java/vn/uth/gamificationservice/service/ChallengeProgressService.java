package vn.uth.gamificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import vn.uth.gamificationservice.client.dto.NotificationRequest;
import vn.uth.gamificationservice.dto.*;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.model.ChallengeApprovalStatus;
import vn.uth.gamificationservice.model.RewardSourceType;
import vn.uth.gamificationservice.model.UserChallengeProgress;
import vn.uth.gamificationservice.repository.ChallengeRepository;
import vn.uth.gamificationservice.repository.UserChallengeProgressRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChallengeProgressService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeProgressService.class);

    private final ChallengeRepository challengeRepository;
    private final UserChallengeProgressRepository progressRepository;
    private final ChallengeRuleEvaluator ruleEvaluator;
    private final RewardService rewardService;
    private final BadgeService badgeService;
    private final RestTemplate notificationRestTemplate;

    public ChallengeProgressService(ChallengeRepository challengeRepository,
                                    UserChallengeProgressRepository progressRepository,
                                    ChallengeRuleEvaluator ruleEvaluator,
                                    @Lazy RewardService rewardService,
                                    BadgeService badgeService,
                                    @Qualifier("notificationRestTemplate") RestTemplate notificationRestTemplate) {
        this.challengeRepository = challengeRepository;
        this.progressRepository = progressRepository;
        this.ruleEvaluator = ruleEvaluator;
        this.rewardService = rewardService;
        this.badgeService = badgeService;
        this.notificationRestTemplate = notificationRestTemplate;
    }

    @Transactional
    public void processEvent(ChallengeEventRequest request) {
        ZonedDateTime now = request.getOccurredAt() != null ? request.getOccurredAt() : ZonedDateTime.now();
        log.debug("Processing challenge event: userId={}, eventType={}, action={}, accuracy={}, score={}",
                request.getUserId(), request.getEventType(), request.getAction(),
                request.getAccuracyPercent(), request.getScore());

        List<Challenge> activeChallenges = challengeRepository
                .findByActiveTrueAndApprovalStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        ChallengeApprovalStatus.APPROVED,
                        now,
                        now);

        for (Challenge challenge : activeChallenges) {
            handleChallenge(challenge, request);
        }
    }

    private void handleChallenge(Challenge challenge, ChallengeEventRequest event) {
        ChallengeRule rule = ruleEvaluator.parse(challenge.getRule());
        if (!ruleEvaluator.matches(rule, event)) {
            log.debug("Event did not match rule for challenge {} ({})", challenge.getId(), challenge.getTitle());
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

        // amount dùng ở rule kiểu "ghi nhận 1 giao dịch", nên mặc định 1 để tránh tăng lệch
        int increment = 1;
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

        // Gửi thông báo Firebase khi challenge hoàn thành
        sendChallengeCompletionNotification(userId, challenge);
    }

    private void sendChallengeCompletionNotification(UUID userId, Challenge challenge) {
        try {
            String title = "🎉 Chúc mừng! Bạn đã hoàn thành thử thách";
            String body = buildNotificationBody(challenge);

            Map<String, Object> data = new HashMap<>();
            data.put("type", "challenge_completed");
            data.put("challengeId", challenge.getId().toString());
            data.put("challengeTitle", challenge.getTitle());
            if (challenge.getRewardScore() != null && challenge.getRewardScore() > 0) {
                data.put("rewardScore", challenge.getRewardScore());
            }
            if (challenge.getRewardBadgeCode() != null && !challenge.getRewardBadgeCode().isEmpty()) {
                data.put("badgeCode", challenge.getRewardBadgeCode());
            }

            NotificationRequest request = new NotificationRequest(title, body, data);
            
            // Gọi notification service bằng RestTemplate (không forward JWT)
            String url = "http://notification-service/api/notifications/user/" + userId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NotificationRequest> entity = new HttpEntity<>(request, headers);
            
            notificationRestTemplate.postForEntity(url, entity, Void.class);
            log.debug("Sent challenge completion notification to user {}", userId);
        } catch (Exception ex) {
            log.error("Failed to send challenge completion notification to user {}", userId, ex);
            // Không throw exception để không ảnh hưởng flow chính
        }
    }

    private String buildNotificationBody(Challenge challenge) {
        StringBuilder body = new StringBuilder();
        body.append("Thử thách \"").append(challenge.getTitle()).append("\" đã được hoàn thành!");

        if (challenge.getRewardScore() != null && challenge.getRewardScore() > 0) {
            body.append(" Bạn nhận được ").append(challenge.getRewardScore()).append(" điểm thưởng.");
        }

        if (challenge.getRewardBadgeCode() != null && !challenge.getRewardBadgeCode().isEmpty()) {
            if (challenge.getRewardScore() != null && challenge.getRewardScore() > 0) {
                body.append(" Và");
            } else {
                body.append(" Bạn");
            }
            body.append(" đã nhận được huy hiệu mới!");
        }

        return body.toString();
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

    public ChallengeSummaryResponse getSummary(UUID userId) {
        List<UserChallengeProgress> progresses = progressRepository.findByUserId(userId);
        List<ChallengeSummaryItem> items = progresses.stream()
                .map(this::toSummaryItem)
                .collect(Collectors.toList());
        long totalChallenges = challengeRepository.countByApprovalStatus(ChallengeApprovalStatus.APPROVED);
        ChallengeSummaryResponse response = new ChallengeSummaryResponse();
        response.setChallenges(items);
        response.setTotalCount(totalChallenges);
        return response;
    }

    private ChallengeSummaryItem toSummaryItem(UserChallengeProgress progress) {
        double percent = 0;
        Integer target = progress.getTargetProgress();
        if (target != null && target > 0) {
            percent = (progress.getCurrentProgress() * 100.0) / target;
        }
        double normalized = Math.min(100.0, Math.max(0.0, percent));
        double rounded = Math.round(normalized * 10.0) / 10.0;
        Challenge challenge = progress.getChallenge();
        String content = challenge.getTitle() != null ? challenge.getTitle() : challenge.getDescription();
        return new ChallengeSummaryItem(challenge.getId(), content, rounded);
    }
}

