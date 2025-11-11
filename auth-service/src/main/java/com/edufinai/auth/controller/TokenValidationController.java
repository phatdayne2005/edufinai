package com.edufinai.auth.controller;

import com.edufinai.auth.dto.ApiResponse;
import com.edufinai.auth.dto.TokenVerificationResponse; // THÊM DÒNG NÀY
import com.edufinai.auth.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Token Validation", description = "APIs for JWT token validation across services")
public class TokenValidationController {

    private final JwtUtil jwtUtil;

    @GetMapping("/verify")
    @Operation(summary = "Verify JWT token for service-to-service communication")
    public ResponseEntity<ApiResponse> verifyToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(ApiResponse.error("Invalid authorization header"));
            }

            String token = authorizationHeader.substring(7);

            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.ok(ApiResponse.error("Invalid or expired token"));
            }

            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token).name();
            String userId = jwtUtil.extractUserId(token).toString();

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("sub", username);
            userInfo.put("role", role);
            userInfo.put("userId", userId);
            userInfo.put("scopes", new String[]{"read", "write"});

            return ResponseEntity.ok(ApiResponse.success("Token is valid", userInfo));

        } catch (Exception e) {
            log.error("Token verification error: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("Token verification failed: " + e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenVerificationResponse> validate(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.ok(new TokenVerificationResponse(false, null, null, null));
        }

        return ResponseEntity.ok(new TokenVerificationResponse(
                true,
                jwtUtil.extractUsername(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUserId(token)
        ));
    }
}