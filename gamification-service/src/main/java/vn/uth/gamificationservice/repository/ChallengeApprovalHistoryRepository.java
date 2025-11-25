package vn.uth.gamificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.uth.gamificationservice.model.ChallengeApprovalHistory;

import java.util.List;
import java.util.UUID;

public interface ChallengeApprovalHistoryRepository extends JpaRepository<ChallengeApprovalHistory, UUID> {
    List<ChallengeApprovalHistory> findByChallenge_IdOrderByCreatedAtDesc(UUID challengeId);

    void deleteByChallenge_Id(UUID challengeId);
}

