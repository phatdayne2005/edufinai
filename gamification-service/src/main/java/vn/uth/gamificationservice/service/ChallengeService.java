package vn.uth.gamificationservice.service;

import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.uth.gamificationservice.dto.ChallengeResponse;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.repository.ChallengeRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ChallengeService {
    private ChallengeRepository challengeRepository;

    public ChallengeService(ChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
    }

    public List<Challenge> findAll() {
        return challengeRepository.findAll();
    }

    @Transactional
    public Challenge save(Challenge newChallenge) {
        return challengeRepository.save(newChallenge);
    }

    @Transactional
    public Challenge delete(UUID challengeId) {
        return challengeRepository.deleteById(challengeId);
    }

    public Challenge findById(UUID id) {
        return this.challengeRepository.findById(id);
    }
}
