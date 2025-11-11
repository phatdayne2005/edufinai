package com.edufinai.auth.service;

import com.edufinai.auth.dto.*;
import com.edufinai.auth.model.User;
import com.edufinai.auth.model.UserRole;
import com.edufinai.auth.model.UserStatus;
import com.edufinai.auth.repository.UserRepository;
import com.edufinai.auth.util.JwtUtil;
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
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

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
            // Check if username already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Username already exists"));
            }

            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Email already exists"));
            }

            // Create new user
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

            // Return response according to API spec
            RegisterResponse registerResponse = new RegisterResponse(
                    savedUser.getUserId(),
                    true // requires_email_verification
            );

            return ResponseEntity.status(201).body(ApiResponse.success("User registered successfully", registerResponse));

        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        try {
            // Tìm user bằng username hoặc email
            User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid username/email or password"));

            // Xác thực với Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            // Chỉ sửa phần login
            String accessToken = jwtUtil.generateAccessToken(
                    user.getUsername(), user.getUserId(), user.getRole(), user.getEmail()
            );
            String refreshToken = jwtUtil.generateRefreshToken(
                    user.getUsername(), user.getUserId(), user.getRole()
            );

            UserProfileResponse profile = new UserProfileResponse(
                    user.getUserId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole(),
                    user.getAvatarUrl(),
                    user.getPreferences(),
                    user.getLastLogin(),
                    user.getStatus(),
                    user.getCreatedAt()
            );

            LoginResponse loginResponse = new LoginResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    jwtUtil.getExpirationTime(),
                    user.getRole(),
                    profile
            );

            return ResponseEntity.ok(loginResponse);

        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            throw new RuntimeException("Invalid username/email or password");
        }
    }

    public ResponseEntity<LoginResponse> refreshToken(RefreshTokenRequest request) {
        try {
            // Validate refresh token
            if (!jwtUtil.validateToken(request.getRefreshToken())) {
                return ResponseEntity.status(401).body(null);
            }

            String username = jwtUtil.extractUsername(request.getRefreshToken());
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate new tokens
            String newAccessToken = jwtUtil.generateAccessToken(
                    user.getUsername(), user.getUserId(), user.getRole(), user.getEmail()
            );
            String newRefreshToken = jwtUtil.generateRefreshToken(
                    user.getUsername(), user.getUserId(), user.getRole()
            );

            UserProfileResponse profile = new UserProfileResponse(
                    user.getUserId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole(),
                    user.getAvatarUrl(),
                    user.getPreferences(),
                    user.getLastLogin(),
                    user.getStatus(),
                    user.getCreatedAt()
            );

            LoginResponse response = new LoginResponse(
                    newAccessToken,
                    newRefreshToken,
                    "Bearer",
                    jwtUtil.getExpirationTime(),
                    user.getRole(),
                    profile
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            return ResponseEntity.status(401).body(null);
        }
    }

    public ResponseEntity<ApiResponse> verifyEmail(VerifyEmailRequest request) {
        try {
            // Implementation for email verification
            return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
        } catch (Exception e) {
            log.error("Email verification error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Email verification failed"));
        }
    }

    public ResponseEntity<ApiResponse> forgotPassword(ForgotPasswordRequest request) {
        try {
            // Implementation for forgot password
            return ResponseEntity.ok(ApiResponse.success("Password reset email sent"));
        } catch (Exception e) {
            log.error("Forgot password error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to process request"));
        }
    }

    public ResponseEntity<ApiResponse> resetPassword(ResetPasswordRequest request) {
        try {
            // Implementation for reset password
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
        } catch (Exception e) {
            log.error("Reset password error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Password reset failed"));
        }
    }

    // Inner class for register response
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