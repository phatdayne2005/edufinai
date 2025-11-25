package vn.uth.learningservice.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.learningservice.model.Learner;
import java.util.List;
import java.util.UUID;

@Repository
public interface LearnerRepository extends JpaRepository<Learner, UUID> {
    List<Learner> findByLevel(Learner.Level level);

    @Modifying
    @Transactional
    @Query("update Learner l set l.totalExp = l.totalExp + :delta where l.id = :learnerId")
    int addExp(@Param("learnerId") UUID learnerId, @Param("delta") long delta);
}
