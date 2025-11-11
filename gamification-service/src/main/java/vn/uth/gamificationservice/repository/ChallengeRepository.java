package vn.uth.gamificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.uth.gamificationservice.model.Challenge;

public interface ChallengeRepository extends JpaRepository<Challenge, Integer> {
    Challenge save(Challenge newChallenge);
}
