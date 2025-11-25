package vn.uth.gamificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.gamificationservice.model.ChallengeScope;
import vn.uth.gamificationservice.model.UserChallengeProgress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserChallengeProgressRepository extends JpaRepository<UserChallengeProgress, UUID> {
    Optional<UserChallengeProgress> findByUserIdAndChallenge_Id(UUID userId, UUID challengeId);

    List<UserChallengeProgress> findByUserIdAndCompletedFalse(UUID userId);

    List<UserChallengeProgress> findByUserIdAndCompletedTrue(UUID userId);

    List<UserChallengeProgress> findByUserId(UUID userId);

    List<UserChallengeProgress> findByChallenge_Scope(ChallengeScope scope);

    void deleteByChallenge_Id(UUID challengeId);
}

