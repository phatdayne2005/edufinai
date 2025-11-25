package vn.uth.gamificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import vn.uth.gamificationservice.dto.LeaderboardType;
import vn.uth.gamificationservice.dto.RewardRequest;
import vn.uth.gamificationservice.dto.RewardResponse;
import vn.uth.gamificationservice.model.Reward;
import vn.uth.gamificationservice.model.RewardSourceType;
import vn.uth.gamificationservice.repository.RewardRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private UserRewardSummaryService userRewardSummaryService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private LeaderboardService leaderboardService;

    @Mock
    private UserService userService;

    @Mock
    private LessonScoreService lessonScoreService;

    @Mock
    private ChallengeEventPublisher challengeEventPublisher;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private RewardService rewardService;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(leaderboardService.getLeaderboardKeyForType(LeaderboardType.DAILY)).thenReturn("leaderboard:daily:test");
        when(leaderboardService.getLeaderboardKeyForType(LeaderboardType.WEEKLY)).thenReturn("leaderboard:weekly:test");
        when(leaderboardService.getLeaderboardKeyForType(LeaderboardType.MONTHLY)).thenReturn("leaderboard:monthly:test");
        when(leaderboardService.getLeaderboardKeyForType(LeaderboardType.ALLTIME)).thenReturn("leaderboard:alltime");
        when(rewardRepository.save(any())).thenAnswer(invocation -> {
            Reward reward = invocation.getArgument(0);
            reward.setRewardId(UUID.randomUUID());
            return reward;
        });
    }

    @Test
    void shouldPersistRewardAndUpdateAllLeaderboards() {
        UUID userId = UUID.randomUUID();
        RewardRequest request = new RewardRequest();
        request.setUserId(userId);
        request.setScore(75);
        request.setSourceType(RewardSourceType.CHALLENGE);
        request.setReason("Weekly challenge completed");

        RewardResponse response = rewardService.addReward(request);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        verify(rewardRepository, times(1)).save(any(Reward.class));
        verify(userRewardSummaryService, times(1)).addSumaryReward(userId, 75);
        verify(zSetOperations, times(1)).incrementScore("leaderboard:daily:test", userId.toString(), 75.0);
        verify(zSetOperations, times(1)).incrementScore("leaderboard:weekly:test", userId.toString(), 75.0);
        verify(zSetOperations, times(1)).incrementScore("leaderboard:monthly:test", userId.toString(), 75.0);
        verify(zSetOperations, times(1)).incrementScore("leaderboard:alltime", userId.toString(), 75.0);
        verifyNoInteractions(lessonScoreService, challengeEventPublisher);
    }

    @Test
    void shouldComputeQuizPointsFromCorrectAnswers() {
        UUID userId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        RewardRequest request = new RewardRequest();
        request.setUserId(userId);
        request.setSourceType(RewardSourceType.QUIZ);
        request.setLessonId(lessonId);
        request.setEnrollId("enroll-123");
        request.setTotalQuestions(5);
        request.setCorrectAnswers(4);

        when(lessonScoreService.processAttempt(eq(userId), eq(lessonId), eq("enroll-123"), eq(40)))
                .thenReturn(new LessonScoreService.LessonAttemptResult(false, 15, 40, 25));

        RewardResponse response = rewardService.addReward(request);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");

        verify(lessonScoreService, times(1))
                .processAttempt(eq(userId), eq(lessonId), eq("enroll-123"), eq(40));
        verify(challengeEventPublisher, times(1))
                .publishLessonCompleted(userId, lessonId, "enroll-123", 40, 80, 5, 4);
        verify(userRewardSummaryService, times(1)).addSumaryReward(userId, 15);
        verify(zSetOperations, times(1)).incrementScore("leaderboard:daily:test", userId.toString(), 15.0);
        verify(zSetOperations, times(1)).incrementScore("leaderboard:weekly:test", userId.toString(), 15.0);
        verify(zSetOperations, times(1)).incrementScore("leaderboard:monthly:test", userId.toString(), 15.0);
        verify(zSetOperations, times(1)).incrementScore("leaderboard:alltime", userId.toString(), 15.0);
    }
}


