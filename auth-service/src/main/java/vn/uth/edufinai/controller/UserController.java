package vn.uth.edufinai.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import vn.uth.edufinai.dto.request.AdminUserCreationRequest;
import vn.uth.edufinai.dto.request.ApiResponse;
import vn.uth.edufinai.dto.request.UserCreationRequest;
import vn.uth.edufinai.dto.request.UserUpdateRequest;
import vn.uth.edufinai.dto.response.UserResponse;
import vn.uth.edufinai.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;

    @PostMapping
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUser(
            @PathVariable("userId") String userId,
            @RequestHeader(value = "X-Internal-Client", required = false) String internalClientHeader) {

        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            // Nếu không phải Admin, kiểm tra Header "X-Internal-Client"
            log.info("Non-admin user accessing /users/{}, checking X-Internal-Client header", userId);
            log.info("X-Internal-Client header value: {}", internalClientHeader);

            if (!"EduFinAI-Internal-Secret".equals(internalClientHeader)) {
                log.warn("Access denied: Invalid or missing X-Internal-Client header");
                throw new org.springframework.security.access.AccessDeniedException(
                        "Access Denied: This endpoint requires admin role or valid internal client header");
            }
            log.info("Access granted via X-Internal-Client header");
        }

        try {
            log.info("Calling userService.getUser for userId: {}", userId);
            UserResponse userResponse = userService.getUser(userId);
            log.info("Successfully retrieved user data for userId: {}", userId);

            var response = ApiResponse.<UserResponse>builder()
                    .result(userResponse)
                    .build();

            log.info("Successfully built response, returning to client");
            return response;
        } catch (Exception e) {
            log.error("Exception occurred while processing getUser request for userId: {}", userId, e);
            throw e;
        }
    }

    @GetMapping("/my-info")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<String> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder().result("User has been deleted").build();
    }

    @PutMapping("/{userId}")
    ApiResponse<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<UserResponse> createUserByAdmin(@RequestBody @Valid AdminUserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUserByAdmin(request))
                .build();
    }
}
