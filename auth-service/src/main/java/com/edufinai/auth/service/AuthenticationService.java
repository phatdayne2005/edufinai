package com.edufinai.auth.service;
import com.edufinai.auth.dto.*;
import com.edufinai.auth.model.User;
import com.edufinai.auth.model.UserRole;
import com.edufinai.auth.model.UserStatus;
import com.edufinai.auth.repository.UserRepository;
import com.edufinai.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    // Lấy ISSUER từ JwtUtil (static)
    private static final String ISSUER = JwtUtil.ISSUER;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthenticationService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 AuthenticationManager authenticationManager,
                                 JwtUtil jwtUtil,
                                 UserDetailsServiceImpl userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public ResponseEntity<ApiResponse> register(RegisterRequest request) {
        try {
            if (userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Username already exists"));
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Email already exists"));
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setDisplayName(request.getDisplayName());
            user.setPhone(request.getPhone());
            user.setRole(UserRole.LEARNER);
            user.setStatus(UserStatus.ACTIVE);

            User savedUser = userRepository.save(user);
            log.info("User registered successfully: {}", savedUser.getUsername());

            RegisterResponse registerResponse = new RegisterResponse(
                    savedUser.getUserId(),
                    true
            );

            return ResponseEntity.status(201)
                    .body(ApiResponse.success("User registered successfully", registerResponse));

        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            String accessToken = jwtUtil.generateAccessToken(
                    user.getUsername(), user.getUserId(), user.getRole(), user.getEmail()
            );
            String refreshToken = jwtUtil.generateRefreshToken(
                    user.getUsername(), user.getUserId(), user.getRole()
            );

            UserProfileResponse profile = new UserProfileResponse(
                    user.getUserId(), user.getEmail(), user.getDisplayName(),
                    user.getRole(), user.getAvatarUrl(), user.getPreferences(),
                    user.getLastLogin(), user.getStatus(), user.getCreatedAt()
            );

            LoginResponse response = new LoginResponse(
                    accessToken, refreshToken, "Bearer",
                    jwtUtil.getExpirationTime(), user.getRole(), profile
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.status(401).body(null);
        }
    }

    @Transactional
    public ResponseEntity<LoginResponse> refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        try {
            // 1. Verify chữ ký + issuer + exp
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401).body(null);
            }

            // 2. Parse claims để lấy type và userId
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtUtil.getRsaKey().toRSAPublicKey())
                    .requireIssuer(ISSUER)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                return ResponseEntity.status(401).body(null);
            }

            // 3. Lấy user từ DB
            UUID userId = UUID.fromString(claims.get("userId", String.class));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 4. Tạo token mới
            String newAccessToken = jwtUtil.generateAccessToken(
                    user.getUsername(), user.getUserId(), user.getRole(), user.getEmail()
            );
            String newRefreshToken = jwtUtil.generateRefreshToken(
                    user.getUsername(), user.getUserId(), user.getRole()
            );

            // 5. Cập nhật lastLogin
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            UserProfileResponse profile = new UserProfileResponse(
                    user.getUserId(), user.getEmail(), user.getDisplayName(),
                    user.getRole(), user.getAvatarUrl(), user.getPreferences(),
                    user.getLastLogin(), user.getStatus(), user.getCreatedAt()
            );

            LoginResponse response = new LoginResponse(
                    newAccessToken, newRefreshToken, "Bearer",
                    jwtUtil.getExpirationTime(), user.getRole(), profile
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Refresh token invalid: {}", e.getMessage());
            return ResponseEntity.status(401).body(null);
        }
    }

    public ResponseEntity<ApiResponse> verifyEmail(VerifyEmailRequest request) {
        // TODO: Implement real logic
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    public ResponseEntity<ApiResponse> forgotPassword(ForgotPasswordRequest request) {
        // TODO: Implement real logic
        return ResponseEntity.ok(ApiResponse.success("Password reset email sent", null));
    }

    public ResponseEntity<ApiResponse> resetPassword(ResetPasswordRequest request) {
        // TODO: Implement real logic
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    // Inner class
    public static class RegisterResponse {
        private UUID userId;
        private boolean requiresEmailVerification;

        public RegisterResponse(UUID userId, boolean requiresEmailVerification) {
            this.userId = userId;
            this.requiresEmailVerification = requiresEmailVerification;
        }

        public UUID getUserId() { return userId; }
        public boolean isRequiresEmailVerification() { return requiresEmailVerification; }
    }
}