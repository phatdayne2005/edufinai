package com.edufinai.auth.dto;

import com.edufinai.auth.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private UserRole role;
    private UserProfileResponse profile;
    private String message = "Login successful";
}