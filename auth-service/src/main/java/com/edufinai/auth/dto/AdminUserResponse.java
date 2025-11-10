package com.edufinai.auth.dto;

import com.edufinai.auth.model.UserRole;
import com.edufinai.auth.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private UUID userId;
    private String email;
    private String displayName;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    // PII-safe version for non-admin access
    public static AdminUserResponse createSafeResponse(AdminUserResponse user) {
        return new AdminUserResponse(
                user.getUserId(),
                null, // Hide email for safety
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getLastLogin(),
                user.getCreatedAt()
        );
    }
}