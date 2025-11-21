package vn.uth.gamificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.gamificationservice.model.UserLessonScore;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLessonScoreRepository extends JpaRepository<UserLessonScore, UUID> {
    Optional<UserLessonScore> findByUserIdAndLessonId(UUID userId, UUID lessonId);
}

