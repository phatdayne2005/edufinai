package vn.uth.firebasenotification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.uth.firebasenotification.entity.FcmToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<FcmToken> findByToken(String token);

    List<FcmToken> findByUserId(UUID userId);

    @Modifying
    @Query("UPDATE FcmToken t SET t.isActive = false WHERE t.token = :token")
    void deactivateByToken(@Param("token") String token);
}
