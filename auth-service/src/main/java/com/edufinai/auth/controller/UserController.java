package com.edufinai.auth.controller;

import com.edufinai.auth.dto.ApiResponse;
import com.edufinai.auth.dto.UpdateProfileRequest;
import com.edufinai.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user profile management")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse> getCurrentUser() {
        return userService.getCurrentUser();
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Operation(summary = "Get user by ID (Admin/Mod only)")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @GetMapping("/profile/{id}")
    @Operation(summary = "Get user profile by ID for services")
    public ResponseEntity<ApiResponse> getUserProfileForService(@PathVariable UUID id) {
        return userService.getUserProfileForService(id);
    }
}

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Management", description = "APIs for admin user management")
class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @Operation(summary = "Get users with filtering and pagination")
    public ResponseEntity<ApiResponse> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.getUsers(role, status, q, page, size);
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<ApiResponse> updateUserRole(@PathVariable UUID id, @RequestBody Map<String, String> request) {
        return userService.updateUserRole(id, request.get("role"));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Update user status")
    public ResponseEntity<ApiResponse> updateUserStatus(@PathVariable UUID id, @RequestBody Map<String, String> request) {
        return userService.updateUserStatus(id, request.get("status"));
    }
}