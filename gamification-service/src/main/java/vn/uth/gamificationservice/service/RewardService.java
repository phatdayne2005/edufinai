package vn.uth.gamificationservice.service;

import org.springframework.stereotype.Service;
import vn.uth.gamificationservice.dto.RewardRequest;

import vn.uth.gamificationservice.dto.RewardResponse;
import vn.uth.gamificationservice.model.Reward;
import vn.uth.gamificationservice.repository.RewardRepository;

import java.time.LocalDateTime;

@Service
public class RewardService {
    private final RewardRepository rewardRepository;

    public RewardService(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    public RewardResponse addReward(RewardRequest req) {
        // Logic validate và lưu DB, update Redis leaderboard.

        Reward reward = new Reward();
        reward.setUserId(req.getUserId());
        reward.setBadge(req.getBadge());
        reward.setScore(req.getScore());
        reward.setReason(req.getReason());
        reward.setCreatedAt(LocalDateTime.now());

        this.rewardRepository.save(reward);

        RewardResponse resp = new RewardResponse(reward.getRewardId(), "SUCCESS");

        return resp;
    }
}
