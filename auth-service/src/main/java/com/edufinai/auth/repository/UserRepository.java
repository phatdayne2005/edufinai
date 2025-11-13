package com.edufinai.auth.repository;

import com.edufinai.auth.model.User;
import com.edufinai.auth.model.UserRole;
import com.edufinai.auth.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // TÌM BẰNG USERNAME HOẶC EMAIL
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    // ĐÃ XÓA: findByRefreshToken → KHÔNG CẦN NỮA
}