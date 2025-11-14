package vn.uth.gamificationservice.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.gamificationservice.dto.*;

import vn.uth.gamificationservice.model.Reward;
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

    public RewardService(RewardRepository rewardRepository,  RedisTemplate<String, String> redisTemplate,  UserRewardSummaryService userRewardSummaryService, LeaderboardService leaderboardService,  UserService userService) {
        this.rewardRepository = rewardRepository;
        this.redisTemplate = redisTemplate;
        this.userRewardSummaryService = userRewardSummaryService;
        this.leaderboardService = leaderboardService;
        this.userService = userService;
    }

    @Transactional
    public RewardResponse addReward(RewardRequest req) {
        // Logic validate và lưu DB, update Redis leaderboard.

        Reward reward = new Reward();
        reward.setUserId(req.getUserId());
        reward.setBadge(req.getBadge());
        reward.setScore(req.getScore());
        reward.setReason(req.getReason());
        reward.setCreatedAt(LocalDateTime.now());

        this.rewardRepository.save(reward);
        this.userRewardSummaryService.addSumaryReward(reward.getUserId(), reward.getScore());

        String userIdStr = req.getUserId().toString();
        double score = req.getScore();

        // Cập nhật tất cả 4 leaderboards: daily, weekly, monthly, alltime
        String dailyKey = leaderboardService.getLeaderboardKeyForType(LeaderboardType.DAILY);
        String weeklyKey = leaderboardService.getLeaderboardKeyForType(LeaderboardType.WEEKLY);
        String monthlyKey = leaderboardService.getLeaderboardKeyForType(LeaderboardType.MONTHLY);
        String alltimeKey = leaderboardService.getLeaderboardKeyForType(LeaderboardType.ALLTIME);

        this.redisTemplate.opsForZSet().incrementScore(dailyKey, userIdStr, score);
        this.redisTemplate.opsForZSet().incrementScore(weeklyKey, userIdStr, score);
        this.redisTemplate.opsForZSet().incrementScore(monthlyKey, userIdStr, score);
        this.redisTemplate.opsForZSet().incrementScore(alltimeKey, userIdStr, score);

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
}
