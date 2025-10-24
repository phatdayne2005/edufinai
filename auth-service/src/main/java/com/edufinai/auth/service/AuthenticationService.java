package com.edufinai.auth.service;

import com.edufinai.auth.dto.*;
import com.edufinai.auth.model.User;
import com.edufinai.auth.model.UserRole;
import com.edufinai.auth.model.UserStatus;
import com.edufinai.auth.repository.UserRepository;
import com.edufinai.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    
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
            
            return ApiResponse.success("User registered successfully", 
                new UserProfileResponse(
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
                ));
                
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
    
    // Inner class for user info response
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoResponse {
        private UUID userId;
        private String username;
        private String email;
        private UserRole role;
        private UserStatus status;
    }
    
    private final UserDetailsServiceImpl userDetailsService;
}