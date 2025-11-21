package vn.uth.learningservice.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.uth.learningservice.model.*;

import java.util.*;

@Repository
public interface ModeratorRepository extends JpaRepository<Moderator, UUID> {
}
