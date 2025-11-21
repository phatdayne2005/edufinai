package vn.uth.learningservice.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.uth.learningservice.model.Creator;

import java.util.*;

@Repository
public interface CreatorRepository extends JpaRepository<Creator, UUID> {

    // đếm số lesson của 1 creator, phục vụ dashboard
    @Query("select count(ls.id) from Lesson ls where ls.creator.id = :creatorId")
    long countLessons(@Param("creatorId") UUID creatorId);
}
