package com.edufinai.auth.repository;

import com.edufinai.auth.model.UserVerification;
import com.edufinai.auth.model.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserVerificationRepository extends JpaRepository<UserVerification, UUID> {
    
    Optional<UserVerification> findByTokenAndVerificationTypeAndIsUsedFalse(String token, VerificationType verificationType);
    
    @Query("SELECT uv FROM UserVerification uv WHERE uv.user.userId = :userId AND uv.verificationType = :type AND uv.isUsed = false AND uv.expiresAt > :now")
    Optional<UserVerification> findActiveVerificationByUserAndType(
            @Param("userId") UUID userId, 
            @Param("type") VerificationType type,
            @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE UserVerification uv SET uv.isUsed = true WHERE uv.user.userId = :userId AND uv.verificationType = :type")
    void invalidatePreviousVerifications(@Param("userId") UUID userId, @Param("type") VerificationType type);
}