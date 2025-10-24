package com.edufinai.auth.controller;

import com.edufinai.auth.dto.ApiResponse;
import com.edufinai.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user profile management")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/profile")
    @Operation(summary = "Get user profile")
    public ResponseEntity<ApiResponse> getUserProfile() {
        ApiResponse response = userService.getUserProfile();
        return ResponseEntity.status(response.isSuccess() ? 200 : 400).body(response);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable UUID userId) {
        ApiResponse response = userService.getUserById(userId);
        return ResponseEntity.status(response.isSuccess() ? 200 : 404).body(response);
    }
}