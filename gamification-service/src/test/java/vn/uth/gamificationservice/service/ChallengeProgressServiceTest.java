package vn.uth.gamificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import vn.uth.gamificationservice.dto.ChallengeEventRequest;
import vn.uth.gamificationservice.dto.ChallengeSummaryItem;
import vn.uth.gamificationservice.dto.ChallengeSummaryResponse;
import vn.uth.gamificationservice.dto.RewardRequest;
import vn.uth.gamificationservice.dto.RewardResponse;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.model.ChallengeApprovalStatus;
import vn.uth.gamificationservice.model.ChallengeScope;
import vn.uth.gamificationservice.model.ChallengeType;
import vn.uth.gamificationservice.model.UserChallengeProgress;
import vn.uth.gamificationservice.repository.ChallengeRepository;
import vn.uth.gamificationservice.repository.UserChallengeProgressRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@Import({ChallengeProgressService.class, ChallengeRuleEvaluator.class, ChallengeProgressServiceTest.TestConfig.class})
class ChallengeProgressServiceTest {

    private static final UUID USER_ONE = UUID.randomUUID();
    private static final UUID USER_TWO = UUID.randomUUID();

    @MockBean
    private RewardService rewardService;

    @MockBean
    private BadgeService badgeService;

    @Autowired
    private ChallengeProgressService challengeProgressService;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private UserChallengeProgressRepository progressRepository;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(rewardService, badgeService);
        when(rewardService.addReward(any())).thenReturn(new RewardResponse(UUID.randomUUID(), "SUCCESS"));
        when(badgeService.awardBadge(any(), any(), any())).thenReturn(null);
    }

    @Test
    void shouldRespectDailyCapWhenMultipleEventsArriveSameDay() {
        Challenge weeklyChallenge = createChallenge(ChallengeScope.WEEKLY, 3, 1, 50, "WEEKLY");
        ChallengeEventRequest event = quizEvent(USER_ONE, 90);

        challengeProgressService.processEvent(event);
        challengeProgressService.processEvent(event); // bị limit 1 lần/ngày

        UserChallengeProgress progress = progressRepository.findByUserIdAndChallenge_Id(USER_ONE, weeklyChallenge.getId())
                .orElseThrow();

        assertThat(progress.getCurrentProgress()).isEqualTo(1);
        assertThat(progress.getProgressCountToday()).isEqualTo(1);
        verify(rewardService, never()).addReward(any());
        verify(badgeService, never()).awardBadge(any(), any(), any());
    }

    @Test
    void shouldCompleteWeeklyChallengeAfterThreeDifferentDays() {
        Challenge weeklyChallenge = createChallenge(ChallengeScope.WEEKLY, 3, 1, 120, "WEEKLY_MASTER");
        ChallengeEventRequest event = quizEvent(USER_ONE, 95);

        for (int day = 0; day < 3; day++) {
            challengeProgressService.processEvent(event);
            if (day < 2) {
                moveProgressToPreviousDay(USER_ONE, weeklyChallenge.getId());
            }
        }

        UserChallengeProgress progress = progressRepository.findByUserIdAndChallenge_Id(USER_ONE, weeklyChallenge.getId())
                .orElseThrow();

        assertThat(progress.getCompleted()).isTrue();
        assertThat(progress.getCurrentProgress()).isEqualTo(3);
        assertThat(progress.getCompletedAt()).isNotNull();

        ArgumentCaptor<RewardRequest> rewardCaptor = ArgumentCaptor.forClass(RewardRequest.class);
        verify(rewardService, times(1)).addReward(rewardCaptor.capture());
        RewardRequest rewardRequest = rewardCaptor.getValue();
        assertThat(rewardRequest.getUserId()).isEqualTo(USER_ONE);
        assertThat(rewardRequest.getScore()).isEqualTo(weeklyChallenge.getRewardScore());
        assertThat(rewardRequest.getChallengeId()).isEqualTo(weeklyChallenge.getId());

        verify(badgeService, times(1)).awardBadge(eq(USER_ONE), eq(weeklyChallenge.getRewardBadgeCode()), eq(weeklyChallenge.getId()));
    }

    @Test
    void shouldIgnoreDuplicatedEventsAfterCompletion() {
        Challenge challenge = createChallenge(ChallengeScope.WEEKLY, 1, 1, 10, "ONCE");
        ChallengeEventRequest event = quizEvent(USER_ONE, 80);

        challengeProgressService.processEvent(event);
        verify(rewardService, times(1)).addReward(any());
        verify(badgeService, times(1)).awardBadge(any(), any(), any());

        challengeProgressService.processEvent(event); // đã hoàn thành nên bỏ qua
        verifyNoMoreInteractions(rewardService);
        verifyNoMoreInteractions(badgeService);
    }

    @Test
    void shouldTrackProgressPerUserIndependently() {
        Challenge challenge = createChallenge(ChallengeScope.DAILY, 2, 2, null, null);
        ChallengeEventRequest eventUserOne = quizEvent(USER_ONE, 85);
        ChallengeEventRequest eventUserTwo = quizEvent(USER_TWO, 90);

        challengeProgressService.processEvent(eventUserOne);
        challengeProgressService.processEvent(eventUserTwo);
        challengeProgressService.processEvent(eventUserTwo); // user two +2 progress

        UserChallengeProgress progressOne = progressRepository.findByUserIdAndChallenge_Id(USER_ONE, challenge.getId()).orElseThrow();
        UserChallengeProgress progressTwo = progressRepository.findByUserIdAndChallenge_Id(USER_TWO, challenge.getId()).orElseThrow();

        assertThat(progressOne.getCurrentProgress()).isEqualTo(1);
        assertThat(progressTwo.getCurrentProgress()).isEqualTo(2);
    }

    @Test
    void shouldSummarizeChallengesForUser() {
        Challenge challenge = createChallenge(ChallengeScope.WEEKLY, 3, 2, null, null);
        ChallengeEventRequest event = quizEvent(USER_ONE, 90);

        challengeProgressService.processEvent(event);
        challengeProgressService.processEvent(event);

        ChallengeSummaryResponse summary = challengeProgressService.getSummary(USER_ONE);
        assertThat(summary.getTotalCount())
                .isEqualTo(challengeRepository.countByApprovalStatus(ChallengeApprovalStatus.APPROVED));
        assertThat(summary.getChallenges()).hasSize(1);

        ChallengeSummaryItem item = summary.getChallenges().get(0);
        assertThat(item.getChallengeId()).isEqualTo(challenge.getId());
        assertThat(item.getContent()).contains(challenge.getTitle());
        assertThat(item.getProgress()).isGreaterThan(60.0);
    }

    @Test
    void shouldSkipEventsThatDoNotMatchRule() {
        Challenge goalChallenge = createGoalChallenge();
        ChallengeEventRequest depositEvent = goalEvent(USER_ONE, "DEPOSIT", 500_000);
        ChallengeEventRequest withdrawEvent = goalEvent(USER_ONE, "WITHDRAW", 200_000);

        challengeProgressService.processEvent(withdrawEvent); // không match action
        assertThat(progressRepository.findByUserIdAndChallenge_Id(USER_ONE, goalChallenge.getId())).isEmpty();

        challengeProgressService.processEvent(depositEvent); // match
        UserChallengeProgress progress = progressRepository.findByUserIdAndChallenge_Id(USER_ONE, goalChallenge.getId()).orElseThrow();
        assertThat(progress.getCurrentProgress()).isEqualTo(1);
    }

    private ChallengeEventRequest quizEvent(UUID userId, int accuracyPercent) {
        ChallengeEventRequest request = new ChallengeEventRequest();
        request.setUserId(userId);
        request.setEventType("QUIZ");
        request.setAction("COMPLETE");
        request.setScore(accuracyPercent);
        request.setAccuracyPercent(accuracyPercent);
        request.setAmount(1);
        request.setOccurredAt(ZonedDateTime.now());
        return request;
    }

    private ChallengeEventRequest goalEvent(UUID userId, String action, int amount) {
        ChallengeEventRequest request = new ChallengeEventRequest();
        request.setUserId(userId);
        request.setEventType("GOAL");
        request.setAction(action);
        request.setAmount(amount);
        request.setOccurredAt(ZonedDateTime.now());
        return request;
    }

    private Challenge createChallenge(ChallengeScope scope, int target, Integer maxPerDay, Integer rewardScore, String badgeCode) {
        Challenge challenge = new Challenge();
        challenge.setTitle(scope + " challenge");
        challenge.setDescription("Test challenge");
        challenge.setType(ChallengeType.QUIZ);
        challenge.setScope(scope);
        challenge.setTargetValue(target);
        challenge.setStartAt(ZonedDateTime.now().minusDays(1));
        challenge.setEndAt(ZonedDateTime.now().plusDays(7));
        challenge.setActive(true);
        challenge.setApprovalStatus(ChallengeApprovalStatus.APPROVED);
        challenge.setRule(ruleJson("QUIZ", "COMPLETE", target, maxPerDay, null, 70));
        challenge.setRewardScore(rewardScore);
        challenge.setRewardBadgeCode(badgeCode);
        challenge.setMaxProgressPerDay(maxPerDay);
        return challengeRepository.save(challenge);
    }

    private Challenge createGoalChallenge() {
        Challenge challenge = new Challenge();
        challenge.setTitle("Goal deposit");
        challenge.setDescription("Deposit money into goal for 3 days");
        challenge.setType(ChallengeType.GOAL);
        challenge.setScope(ChallengeScope.MONTHLY);
        challenge.setTargetValue(3);
        challenge.setStartAt(ZonedDateTime.now().minusDays(1));
        challenge.setEndAt(ZonedDateTime.now().plusDays(40));
        challenge.setActive(true);
        challenge.setRule(ruleJson("GOAL", "DEPOSIT", 3, 1, null, null));
        challenge.setRewardScore(300);
        challenge.setRewardBadgeCode("SAVER");
        challenge.setMaxProgressPerDay(1);
        return challengeRepository.save(challenge);
    }

    private void moveProgressToPreviousDay(UUID userId, UUID challengeId) {
        UserChallengeProgress progress = progressRepository.findByUserIdAndChallenge_Id(userId, challengeId).orElseThrow();
        progress.setLastProgressDate(LocalDate.now().minusDays(1));
        progress.setProgressCountToday(0);
        progressRepository.saveAndFlush(progress);
    }

    private String ruleJson(String eventType, String action, Integer count, Integer maxPerDay, Integer minScore, Integer minAccuracy) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("action", action);
        if (count != null) {
            payload.put("count", count);
        }
        if (maxPerDay != null) {
            payload.put("maxProgressPerDay", maxPerDay);
        }
        if (minScore != null) {
            payload.put("minScore", minScore);
        }
        if (minAccuracy != null) {
            payload.put("minAccuracy", minAccuracy);
        }
        try {
            return new ObjectMapper().writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}


