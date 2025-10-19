package vn.uth.gamificationservice.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.gamificationservice.dto.RewardRequest;

import vn.uth.gamificationservice.dto.RewardResponse;
import vn.uth.gamificationservice.model.Reward;
import vn.uth.gamificationservice.repository.RewardRepository;

import java.time.LocalDateTime;

@Service
public class RewardService {
    private final RewardRepository rewardRepository;

    private final RedisTemplate<String, String> redisTemplate;

    private static final String LEADERBOARD_KEY = "leaderboard:global";

    public RewardService(RewardRepository rewardRepository,  RedisTemplate<String, String> redisTemplate) {
        this.rewardRepository = rewardRepository;
        this.redisTemplate = redisTemplate;
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

        // Cộng điểm hiện tại với điểm mới (nếu không có sẽ thêm)
        // ZINCRBY có thể dùng hoặc lấy điểm hiện tại rồi cộng thủ công
        this.redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, req.getUserId().toString(), req.getScore());

        RewardResponse resp = new RewardResponse(reward.getRewardId(), "SUCCESS");

        return resp;
    }
}
