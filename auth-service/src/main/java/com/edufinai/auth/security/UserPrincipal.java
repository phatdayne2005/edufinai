// src/main/java/com/edufinai/auth/security/UserPrincipal.java
package com.edufinai.auth.security;

import com.edufinai.auth.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserPrincipal {
    private UUID userId;
    private String username;
    private String email;
    private UserRole role;
}