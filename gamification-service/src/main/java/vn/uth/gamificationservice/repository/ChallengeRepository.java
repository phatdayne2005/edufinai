package vn.uth.gamificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.model.ChallengeApprovalStatus;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {
    List<Challenge> findByActiveTrueAndApprovalStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            ChallengeApprovalStatus approvalStatus,
            ZonedDateTime startAt,
            ZonedDateTime endAt);

    List<Challenge> findByApprovalStatus(ChallengeApprovalStatus approvalStatus);

    long countByApprovalStatus(ChallengeApprovalStatus approvalStatus);
}
