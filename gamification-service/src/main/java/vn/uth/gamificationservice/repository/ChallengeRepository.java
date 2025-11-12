package vn.uth.gamificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.uth.gamificationservice.model.Challenge;

import java.util.UUID;

public interface ChallengeRepository extends JpaRepository<Challenge, Integer> {
    Challenge save(Challenge newChallenge);
    Challenge findById(UUID id);
    Challenge deleteById(UUID id);
}
