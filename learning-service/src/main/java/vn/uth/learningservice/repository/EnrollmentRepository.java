package vn.uth.learningservice.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.learningservice.model.Enrollment;

import java.util.*;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    // vì (learner, lesson) là unique -> dùng để get nhanh enrollment hiện hữu
    Optional<Enrollment> findByLearner_IdAndLesson_Id(UUID learnerId, UUID lessonId);

    List<Enrollment> findByLearner_Id(UUID learnerId);
    List<Enrollment> findByLesson_Id(UUID lessonId);

    boolean existsByLearner_IdAndLesson_Id(UUID learnerId, UUID lessonId);

    // cập nhật tiến độ/điểm/attempts (bám theo ràng buộc 0..100 cho progress/score)
    @Modifying @Transactional
    @Query("""
           update Enrollment e
              set e.status = :status,
                  e.progressPercent = :progress,
                  e.score = :score,
                  e.attempts = e.attempts + :addAttempt,
                  e.lastActivityAt = CURRENT_TIMESTAMP
            where e.id = :enrollId
           """)
    int updateProgress(@Param("enrollId") UUID enrollId,
                       @Param("status") Enrollment.Status status,
                       @Param("progress") int progress,
                       @Param("score") Integer score,
                       @Param("addAttempt") int addAttempt);

    // đếm số lesson đã COMPLETED của 1 learner
    @Query("select count(e.id) from Enrollment e where e.learner.id = :learnerId and e.status = 'COMPLETED'")
    long countCompletedByLearner(@Param("learnerId") UUID learnerId);
}
