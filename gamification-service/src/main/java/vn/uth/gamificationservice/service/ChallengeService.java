package vn.uth.gamificationservice.service;

import org.springframework.stereotype.Service;
import vn.uth.gamificationservice.dto.ChallengeResponse;
import vn.uth.gamificationservice.model.Challenge;
import vn.uth.gamificationservice.repository.ChallengeRepository;

import java.util.List;

@Service
public class ChallengeService {
    private ChallengeRepository challengeRepository;

    public ChallengeService(ChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
    }

    public List<Challenge> findAll() {
        return challengeRepository.findAll();
    }

    public ChallengeResponse save(Challenge newChallenge) {
        challengeRepository.save(newChallenge);
        return new ChallengeResponse(newChallenge.getId(), "SUCCESS");
    }
}
