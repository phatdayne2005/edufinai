package vn.uth.gamificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.gamificationservice.model.UserBadge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    Optional<UserBadge> findByUserIdAndBadge_Id(UUID userId, UUID badgeId);

    List<UserBadge> findByUserId(UUID userId);
}

