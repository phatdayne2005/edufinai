package vn.uth.financeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.financeservice.entity.UserBalance;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBalanceRepository extends JpaRepository<UserBalance, UUID> {
    Optional<UserBalance> findByUserId(UUID userId);
}

