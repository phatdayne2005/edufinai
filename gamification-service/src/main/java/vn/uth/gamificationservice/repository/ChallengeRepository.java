package vn.uth.gamificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.gamificationservice.model.Challenge;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {
    List<Challenge> findByActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqual(ZonedDateTime from, ZonedDateTime to);
}
