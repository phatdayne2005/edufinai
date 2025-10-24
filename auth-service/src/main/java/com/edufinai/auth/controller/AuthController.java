package com.edufinai.auth.controller;

import com.edufinai.auth.dto.ApiResponse;
import com.edufinai.auth.dto.LoginRequest;
import com.edufinai.auth.dto.RegisterRequest;
import com.edufinai.auth.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs for user registration and login")
public class AuthController {
    
    private final AuthenticationService authenticationService;
    
    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        ApiResponse response = authenticationService.register(request);
        return ResponseEntity.status(response.isSuccess() ? 200 : 400).body(response);
    }
    
    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        ApiResponse response = authenticationService.login(request);
        return ResponseEntity.status(response.isSuccess() ? 200 : 401).body(response);
    }
    
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse> getCurrentUser() {
        ApiResponse response = authenticationService.getCurrentUser();
        return ResponseEntity.status(response.isSuccess() ? 200 : 400).body(response);
    }
    
    @GetMapping("/user/info")
    @Operation(summary = "Get user info by token (for internal services)")
    public ResponseEntity<ApiResponse> getUserInfo(@RequestHeader("Authorization") String token) {
        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        ApiResponse response = authenticationService.getUserInfo(token);
        return ResponseEntity.status(response.isSuccess() ? 200 : 400).body(response);
    }
}