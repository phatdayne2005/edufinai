package vn.uth.edufinai.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.uth.edufinai.constant.PredefinedRole;
import vn.uth.edufinai.dto.request.AdminUserCreationRequest;
import vn.uth.edufinai.dto.request.UserCreationRequest;
import vn.uth.edufinai.dto.request.UserUpdateRequest;
import vn.uth.edufinai.dto.response.UserResponse;
import vn.uth.edufinai.entity.Role;
import vn.uth.edufinai.entity.User;
import vn.uth.edufinai.exception.AppException;
import vn.uth.edufinai.exception.ErrorCode;
import vn.uth.edufinai.mapper.UserMapper;
import vn.uth.edufinai.repository.RoleRepository;
import vn.uth.edufinai.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserCreationRequest request) {
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.LEARNER_ROLE).ifPresent(roles::add);

        user.setRoles(roles);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        return userMapper.toUserResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUserByAdmin(AdminUserCreationRequest request) {
        // Validate role
        String roleName = request.getRole();
        if (!roleName.equals(PredefinedRole.LEARNER_ROLE)
                && !roleName.equals(PredefinedRole.CREATOR_ROLE)
                && !roleName.equals(PredefinedRole.MOD_ROLE)
                && !roleName.equals(PredefinedRole.ADMIN_ROLE)) {
            throw new AppException(ErrorCode.INVALID_KEY); // Using INVALID_KEY for invalid role
        }

        // Manually create User from AdminUserCreationRequest
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dob(request.getDob())
                .build();

        // Set the role specified by admin
        HashSet<Role> roles = new HashSet<>();
        Role role = roleRepository.findById(roleName)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        roles.add(role);
        user.setRoles(roles);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        return userMapper.toUserResponse(user);
    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Check if current user is admin or updating their own account
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
        boolean isOwnAccount = authentication.getName().equals(user.getUsername());

        if (!isAdmin && !isOwnAccount) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // IMPORTANT: Only update fields that are explicitly provided in the request
        // If a field is null in the request, it means it was not included, so we don't
        // update it
        // This prevents data loss when updating only specific fields

        // Update firstName only if provided (not null)
        if (request.getFirstName() != null) {
            String firstName = request.getFirstName().trim();
            user.setFirstName(firstName.isEmpty() ? null : firstName);
        }
        // If firstName is null in request, keep existing value (don't update)

        // Update lastName only if provided (not null)
        if (request.getLastName() != null) {
            String lastName = request.getLastName().trim();
            user.setLastName(lastName.isEmpty() ? null : lastName);
        }
        // If lastName is null in request, keep existing value (don't update)

        // Update email only if provided (not null)
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            user.setEmail(email.isEmpty() ? null : email);
        }
        // If email is null in request, keep existing value (don't update)

        // Update phone only if provided (not null)
        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            user.setPhone(phone.isEmpty() ? null : phone);
        }
        // If phone is null in request, keep existing value (don't update)

        // Update dob only if provided (not null)
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
        // If dob is null in request, keep existing value (don't update)

        // Only update password if provided and not empty
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        // If password is null in request, keep existing password (don't update)

        // Only update roles if provided and not empty
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            var roles = roleRepository.findAllById(request.getRoles());
            user.setRoles(new HashSet<>(roles));
        }
        // If roles is null in request, keep existing roles (don't update)

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsers() {
        log.info("In method get Users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    // Authorization is handled at controller level with custom logic
    // that checks for ADMIN role OR X-Internal-Client header
    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }
}
