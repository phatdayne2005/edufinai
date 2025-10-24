package com.edufinai.auth.service;

import com.edufinai.auth.dto.ApiResponse;
import com.edufinai.auth.dto.UserProfileResponse;
import com.edufinai.auth.model.User;
import com.edufinai.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public ApiResponse getUserProfile() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            
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
            log.error("Error getting user profile: {}", e.getMessage());
            return ApiResponse.error("Failed to get user profile");
        }
    }
    
    public ApiResponse getUserById(UUID userId) {
        try {
            Optional<User> userOptional = userRepository.findActiveUserById(userId);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
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
            } else {
                return ApiResponse.error("User not found");
            }
            
        } catch (Exception e) {
            log.error("Error getting user by ID: {}", e.getMessage());
            return ApiResponse.error("Failed to get user profile");
        }
    }
}