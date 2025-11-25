package vn.uth.gamificationservice.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.gamificationservice.dto.LeaderboardType;
import vn.uth.gamificationservice.dto.RewardRequest;
import vn.uth.gamificationservice.dto.RewardResponse;
import vn.uth.gamificationservice.dto.UserInfo;
import vn.uth.gamificationservice.dto.UserReward;
import vn.uth.gamificationservice.model.Reward;
import vn.uth.gamificationservice.model.RewardSourceType;
import vn.uth.gamificationservice.repository.RewardRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RewardService {
    private final RewardRepository rewardRepository;
    private final UserRewardSummaryService userRewardSummaryService;
    private final RedisTemplate<String, String> redisTemplate;
    private final LeaderboardService leaderboardService;
    private final UserService userService;
    private final LessonScoreService lessonScoreService;
    private final ChallengeEventPublisher challengeEventPublisher;

    public RewardService(RewardRepository rewardRepository,
                         RedisTemplate<String, String> redisTemplate,
                         UserRewardSummaryService userRewardSummaryService,
                         LeaderboardService leaderboardService,
                         UserService userService,
                         LessonScoreService lessonScoreService,
                         ChallengeEventPublisher challengeEventPublisher) {
        this.rewardRepository = rewardRepository;
        this.redisTemplate = redisTemplate;
        this.userRewardSummaryService = userRewardSummaryService;
        this.leaderboardService = leaderboardService;
        this.userService = userService;
        this.lessonScoreService = lessonScoreService;
        this.challengeEventPublisher = challengeEventPublisher;
    }

    @Transactional
    public RewardResponse addReward(RewardRequest req) {
        RewardSourceType sourceType = req.getSourceType() != null ? req.getSourceType() : RewardSourceType.MANUAL;
        UUID userId = req.getUserId();
        int pointsToAdd = req.getScore() != null ? req.getScore() : 0;

        QuizPayload quizPayload = null;

        if (sourceType == RewardSourceType.QUIZ) {
            validateLessonPayload(req);
            quizPayload = buildQuizPayload(req);

            LessonScoreService.LessonAttemptResult attemptResult =
                    lessonScoreService.processAttempt(userId, req.getLessonId(), req.getEnrollId(), quizPayload.points());

            if (attemptResult.duplicate()) {
                return new RewardResponse(null, "DUPLICATE_ATTEMPT");
            }

            challengeEventPublisher.publishLessonCompleted(
                    userId,
                    req.getLessonId(),
                    req.getEnrollId(),
                    quizPayload.points(),
                    quizPayload.accuracyPercent(),
                    quizPayload.totalQuestions(),
                    quizPayload.correctAnswers());

            if (!attemptResult.hasImproved()) {
                return new RewardResponse(null, "NO_SCORE_CHANGE");
            }

            pointsToAdd = attemptResult.deltaScore();
        }

        if (pointsToAdd <= 0) {
            return new RewardResponse(null, "NO_SCORE_CHANGE");
        }

        Reward reward = new Reward();
        reward.setUserId(userId);
        reward.setBadge(req.getBadge());
        reward.setScore(pointsToAdd);
        reward.setReason(req.getReason());
        reward.setSourceType(sourceType);
        reward.setLessonId(req.getLessonId());
        reward.setEnrollId(req.getEnrollId());
        reward.setChallengeId(req.getChallengeId());
        reward.setCreatedAt(LocalDateTime.now());

        this.rewardRepository.save(reward);
        this.userRewardSummaryService.addSumaryReward(userId, pointsToAdd);

        updateLeaderboards(userId, pointsToAdd);

        if (sourceType == RewardSourceType.QUIZ) {
            // Challenge event already published earlier.
        }

        return new RewardResponse(reward.getRewardId(), "SUCCESS");
    }

    public UserReward getUserReward() {
        // Lấy thông tin user
        UserInfo userInfo = this.userService.getMyInfo();
        UUID userId = userInfo.getId();

        // Lấy điểm từ alltime leaderboard
        String alltimeKey = leaderboardService.getLeaderboardKeyForType(LeaderboardType.ALLTIME);
        Double score = redisTemplate.opsForZSet().score(alltimeKey, userId.toString());
        if (score == null) {
            score = 0.0;
        }
        List<Reward> rewardDetail = this.rewardRepository.findByUserId(userId);

        return new UserReward(userId, score, rewardDetail);
    }

    private void validateLessonPayload(RewardRequest req) {
        if (req.getLessonId() == null) {
            throw new IllegalArgumentException("lessonId is required for quiz rewards");
        }
        if (req.getEnrollId() == null || req.getEnrollId().isBlank()) {
            throw new IllegalArgumentException("enrollId is required for quiz rewards");
        }
    }

    private QuizPayload buildQuizPayload(RewardRequest req) {
        Integer totalQuestions = req.getTotalQuestions();
        Integer correctAnswers = req.getCorrectAnswers();

        if (totalQuestions == null || totalQuestions <= 0) {
            throw new IllegalArgumentException("totalQuestions must be greater than 0 for quiz rewards");
        }
        if (correctAnswers == null || correctAnswers < 0 || correctAnswers > totalQuestions) {
            throw new IllegalArgumentException("correctAnswers must be between 0 and totalQuestions");
        }

        int points = correctAnswers * 10;
        int accuracyPercent = (int) Math.round((correctAnswers * 100.0) / totalQuestions);

        return new QuizPayload(totalQuestions, correctAnswers, points, accuracyPercent);
    }

    private void updateLeaderboards(UUID userId, double delta) {
        String userIdStr = userId.toString();

        String dailyKey = leaderboardService.getLeaderboardKeyForType(LeaderboardType.DAILY);
        String weeklyKey = leaderboardService.getLeaderboardKeyForType(LeaderboardType.WEEKLY);
        String monthlyKey = leaderboardService.getLeaderboardKeyForType(LeaderboardType.MONTHLY);
        String alltimeKey = leaderboardService.getLeaderboardKeyForType(LeaderboardType.ALLTIME);

        this.redisTemplate.opsForZSet().incrementScore(dailyKey, userIdStr, delta);
        this.redisTemplate.opsForZSet().incrementScore(weeklyKey, userIdStr, delta);
        this.redisTemplate.opsForZSet().incrementScore(monthlyKey, userIdStr, delta);
        this.redisTemplate.opsForZSet().incrementScore(alltimeKey, userIdStr, delta);
    }

    private record QuizPayload(int totalQuestions, int correctAnswers, int points, int accuracyPercent) {
    }
}
