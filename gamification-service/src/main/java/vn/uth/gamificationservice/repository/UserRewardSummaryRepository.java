package vn.uth.gamificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.gamificationservice.model.UserRewardSummary;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRewardSummaryRepository extends JpaRepository<UserRewardSummary, UUID> {
    UserRewardSummary save(UserRewardSummary userRewardSummary);
    List<UserRewardSummary> findAllByUserId(UUID userId);
    UserRewardSummary findByUserId(UUID userId);
}
