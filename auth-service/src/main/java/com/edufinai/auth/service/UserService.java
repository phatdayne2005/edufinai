package com.edufinai.auth.service;

import com.edufinai.auth.dto.AdminUserResponse;
import com.edufinai.auth.dto.ApiResponse;
import com.edufinai.auth.dto.UpdateProfileRequest;
import com.edufinai.auth.model.User;
import com.edufinai.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public ResponseEntity<ApiResponse> getCurrentUser() {
        return ResponseEntity.ok(ApiResponse.success("Not implemented yet", null));
    }

    public ResponseEntity<ApiResponse> updateProfile(UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Not implemented yet", null));
    }

    public ResponseEntity<ApiResponse> getUserById(UUID id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()) ||
                            "ROLE_MODERATOR".equals(auth.getAuthority()));

            AdminUserResponse response;
            if (isAdmin) {
                response = new AdminUserResponse(
                        user.getUserId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole(),
                        user.getStatus(),
                        user.getLastLogin(),
                        user.getCreatedAt()
                );
            } else {
                response = AdminUserResponse.createSafeResponse(
                        new AdminUserResponse(
                                user.getUserId(),
                                user.getEmail(),
                                user.getDisplayName(),
                                user.getRole(),
                                user.getStatus(),
                                user.getLastLogin(),
                                user.getCreatedAt()
                        )
                );
            }

            return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));

        } catch (Exception e) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found"));
        }
    }

    public ResponseEntity<ApiResponse> getUserProfileForService(UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Not implemented yet", null));
    }

    public ResponseEntity<ApiResponse> getUsers(String role, String status, String q, int page, int size) {
        return ResponseEntity.ok(ApiResponse.success("Not implemented yet", null));
    }

    public ResponseEntity<ApiResponse> updateUserRole(UUID id, String role) {
        return ResponseEntity.ok(ApiResponse.success("Not implemented yet", null));
    }

    public ResponseEntity<ApiResponse> updateUserStatus(UUID id, String status) {
        return ResponseEntity.ok(ApiResponse.success("Not implemented yet", null));
    }
}