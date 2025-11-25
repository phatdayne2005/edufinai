package vn.uth.gamificationservice.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.model.ChallengeApprovalHistory;
import vn.uth.gamificationservice.model.ChallengeApprovalStatus;
import vn.uth.gamificationservice.repository.ChallengeApprovalHistoryRepository;
import vn.uth.gamificationservice.repository.ChallengeRepository;
import vn.uth.gamificationservice.repository.UserChallengeProgressRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChallengeService {
    private final ChallengeRepository challengeRepository;
    private final UserChallengeProgressRepository progressRepository;
    private final ChallengeApprovalHistoryRepository approvalHistoryRepository;

    public ChallengeService(ChallengeRepository challengeRepository,
                            UserChallengeProgressRepository progressRepository,
                            ChallengeApprovalHistoryRepository approvalHistoryRepository) {
        this.challengeRepository = challengeRepository;
        this.progressRepository = progressRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
    }

    public List<Challenge> findAll() {
        return challengeRepository.findAll();
    }

    @Transactional
    public Challenge save(Challenge newChallenge) {
        if (newChallenge.getApprovalStatus() == null) {
            newChallenge.setApprovalStatus(ChallengeApprovalStatus.PENDING);
        }
        return challengeRepository.save(newChallenge);
    }

    @Transactional
    public void delete(UUID challengeId) {
        progressRepository.deleteByChallenge_Id(challengeId);
        approvalHistoryRepository.deleteByChallenge_Id(challengeId);
        challengeRepository.deleteById(challengeId);
    }

    public Challenge findById(UUID id) {
        return this.challengeRepository.findById(id).orElse(null);
    }

    public boolean existsById(UUID id) {
        return this.challengeRepository.existsById(id);
    }

    public List<Challenge> findByApprovalStatus(ChallengeApprovalStatus status) {
        return this.challengeRepository.findByApprovalStatus(status);
    }

    @Transactional
    public Challenge updateApprovalStatus(UUID id, ChallengeApprovalStatus status, UUID reviewerId, String note) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found: " + id));
        challenge.setApprovalStatus(status);
        if (status != ChallengeApprovalStatus.APPROVED) {
            challenge.setActive(false);
        }
        Challenge updated = challengeRepository.save(challenge);
        saveHistory(challenge, status, reviewerId, note);
        return updated;
    }

    @Transactional
    public Challenge resubmit(UUID id, UUID requesterId) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found: " + id));
        challenge.setApprovalStatus(ChallengeApprovalStatus.PENDING);
        challenge.setActive(false);
        Challenge updated = challengeRepository.save(challenge);
        saveHistory(challenge, ChallengeApprovalStatus.PENDING, requesterId, "Resubmitted");
        return updated;
    }

    public List<ChallengeApprovalHistory> getApprovalHistory(UUID challengeId) {
        return approvalHistoryRepository.findByChallenge_IdOrderByCreatedAtDesc(challengeId);
    }

    private void saveHistory(Challenge challenge,
                             ChallengeApprovalStatus status,
                             UUID reviewerId,
                             String note) {
        ChallengeApprovalHistory history = new ChallengeApprovalHistory();
        history.setChallenge(challenge);
        history.setReviewerId(reviewerId);
        history.setStatus(status);
        history.setNote(note);
        history.setCreatedAt(ZonedDateTime.now());
        approvalHistoryRepository.save(history);
    }
}
