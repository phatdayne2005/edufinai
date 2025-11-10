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
public class UserProfileResponse {
    private UUID userId;
    private String email;
    private String displayName;
    private UserRole role;
    private String avatarUrl;
    private String preferences;
    private LocalDateTime lastLogin;
    private UserStatus status;
    private LocalDateTime createdAt;
}