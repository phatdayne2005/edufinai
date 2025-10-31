package com.edufinai.auth.controller;

import com.edufinai.auth.dto.ApiResponse;
import com.edufinai.auth.model.User;
import com.edufinai.auth.repository.UserRepository;
import com.edufinai.auth.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Token Validation", description = "APIs for JWT token validation across services")
public class TokenValidationController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/validate")
    @Operation(summary = "Validate JWT token and return user info")
    public ResponseEntity<ApiResponse> validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            // Extract token from "Bearer {token}" format
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(ApiResponse.error("Invalid authorization header"));
            }

            String token = authorizationHeader.substring(7);

            // SỬA LẠI: Sử dụng method validateToken mới chỉ cần token
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.ok(ApiResponse.error("Invalid or expired token"));
            }

            // Extract user info from token
            String username = jwtUtil.extractUsername(token);
            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("User not found"));
            }

            User user = userOptional.get();

            // Create response with user info
            TokenValidationResponse response = new TokenValidationResponse(
                    user.getUserId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus()
            );

            return ResponseEntity.ok(ApiResponse.success("Token is valid", response));

        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("Token validation failed: " + e.getMessage()));
        }
    }

    @GetMapping("/public-key")
    @Operation(summary = "Get public key for token verification (if using asymmetric encryption)")
    public ResponseEntity<ApiResponse> getPublicKey() {
        // For future use with RSA encryption
        return ResponseEntity.ok(ApiResponse.success("Using symmetric key", null));
    }

    // Response DTO
    private static class TokenValidationResponse {
        private final String userId;
        private final String username;
        private final String email;
        private final String role;
        private final String status;

        public TokenValidationResponse(Object userId, String username, String email, Object role, Object status) {
            this.userId = userId.toString();
            this.username = username;
            this.email = email;
            this.role = role.toString();
            this.status = status.toString();
        }

        // Getters
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
    }
}