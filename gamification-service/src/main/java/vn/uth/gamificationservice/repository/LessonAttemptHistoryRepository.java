package vn.uth.gamificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.gamificationservice.model.LessonAttemptHistory;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonAttemptHistoryRepository extends JpaRepository<LessonAttemptHistory, UUID> {
    boolean existsByEnrollId(String enrollId);
}

