package com.edufinai.auth.service;

import com.edufinai.auth.dto.*;
import com.edufinai.auth.model.User;
import com.edufinai.auth.model.UserRole;
import com.edufinai.auth.model.UserStatus;
import com.edufinai.auth.repository.UserRepository;
import com.edufinai.auth.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public ApiResponse register(RegisterRequest request) {
        try {
            // Check if username already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                return ApiResponse.error("Username already exists");
            }

            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return ApiResponse.error("Email already exists");
            }

            // Create new user
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setPhone(request.getPhone());
            user.setRole(request.getRole());
            user.setStatus(UserStatus.ACTIVE);
            user.setCreatedAt(LocalDateTime.now());

            User savedUser = userRepository.save(user);
            log.info("User registered successfully: {}", savedUser.getUsername());

            UserProfileResponse profileResponse = new UserProfileResponse(
                    savedUser.getUserId(),
                    savedUser.getUsername(),
                    savedUser.getEmail(),
                    savedUser.getPhone(),
                    savedUser.getRole(),
                    savedUser.getAvatarUrl(),
                    savedUser.getFinanceProfile(),
                    savedUser.getGoals(),
                    savedUser.getLastLogin(),
                    savedUser.getStatus(),
                    savedUser.getCreatedAt()
            );

            return ApiResponse.success("User registered successfully", profileResponse);

        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return ApiResponse.error("Registration failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            String jwt = jwtUtil.generateToken(user.getUsername(), user.getRole());

            UserProfileResponse profile = new UserProfileResponse(
                    user.getUserId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.getAvatarUrl(),
                    user.getFinanceProfile(),
                    user.getGoals(),
                    user.getLastLogin(),
                    user.getStatus(),
                    user.getCreatedAt()
            );

            LoginResponse loginResponse = new LoginResponse(jwt, "Bearer", user.getRole(), profile, "Login successful");

            return ApiResponse.success("Login successful", loginResponse);

        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            return ApiResponse.error("Invalid username or password");
        }
    }

    public ApiResponse getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserProfileResponse profile = new UserProfileResponse(
                    user.getUserId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.getAvatarUrl(),
                    user.getFinanceProfile(),
                    user.getGoals(),
                    user.getLastLogin(),
                    user.getStatus(),
                    user.getCreatedAt()
            );

            return ApiResponse.success("User profile retrieved successfully", profile);

        } catch (Exception e) {
            return ApiResponse.error("Failed to get user profile");
        }
    }

    public ApiResponse getUserInfo(String token) {
        try {
            if (jwtUtil.validateToken(token, userDetailsService.loadUserByUsername(jwtUtil.extractUsername(token)))) {
                String username = jwtUtil.extractUsername(token);
                UserRole role = jwtUtil.extractRole(token);

                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                UserInfoResponse userInfo = new UserInfoResponse(
                        user.getUserId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole(),
                        user.getStatus()
                );

                return ApiResponse.success("User info retrieved successfully", userInfo);
            } else {
                return ApiResponse.error("Invalid token");
            }
        } catch (Exception e) {
            return ApiResponse.error("Failed to get user info");
        }
    }

    // Inner class for user info response (không dùng Lombok)
    public static class UserInfoResponse {
        private UUID userId;
        private String username;
        private String email;
        private UserRole role;
        private UserStatus status;

        // Constructor mặc định
        public UserInfoResponse() {}

        // Constructor với tham số
        public UserInfoResponse(UUID userId, String username, String email, UserRole role, UserStatus status) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.role = role;
            this.status = status;
        }

        // Getters and Setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
        public UserStatus getStatus() { return status; }
        public void setStatus(UserStatus status) { this.status = status; }
    }
}