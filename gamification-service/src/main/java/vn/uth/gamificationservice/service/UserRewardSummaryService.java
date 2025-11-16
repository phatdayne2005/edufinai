package vn.uth.gamificationservice.service;

import org.springframework.stereotype.Service;
import vn.uth.gamificationservice.model.UserRewardSummary;
import vn.uth.gamificationservice.repository.UserRewardSummaryRepository;

import java.util.List;
import java.util.UUID;

@Service
public class UserRewardSummaryService {
    private final UserRewardSummaryRepository userRewardSummaryRepository;
    public UserRewardSummaryService(UserRewardSummaryRepository userRewardSummaryRepository) {
        this.userRewardSummaryRepository = userRewardSummaryRepository;
    }

    public List<UserRewardSummary> findAllByUserId(UUID userId) {
        return this.userRewardSummaryRepository.findAllByUserId(userId);
    }

    public UserRewardSummary findByUserId(UUID userId) {
        return this.userRewardSummaryRepository.findByUserId(userId);
    }

    public UserRewardSummary save(UserRewardSummary userRewardSummary) {
        return this.userRewardSummaryRepository.save(userRewardSummary);
    }

    public UserRewardSummary addSumaryReward(UUID userId, double delta) {
        UserRewardSummary summary = this.userRewardSummaryRepository.findByUserId(userId);
        if (summary != null) {
            summary.setTotalScore(summary.getTotalScore() + delta);
        }
        else  {
            summary = new UserRewardSummary();
            summary.setUserId(userId);
            summary.setTotalScore(delta);
        }
        return this.userRewardSummaryRepository.save(summary);
    }
}
