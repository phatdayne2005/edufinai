package vn.uth.financeservice.repository;

import vn.uth.financeservice.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
public interface GoalRepository extends JpaRepository<Goal, UUID>{
    List<Goal> findByUserId(UUID userId);
}
