package vn.uth.edufinai.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.uth.edufinai.validator.DobConstraint;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserCreationRequest {
    @Size(min = 4, message = "USERNAME_INVALID")
    String username;

    @Size(min = 6, message = "INVALID_PASSWORD")
    String password;

    String firstName;
    String lastName;
    
    @Email(message = "INVALID_EMAIL")
    String email;

    @Size(min = 6, max = 20, message = "INVALID_PHONE")
    String phone;
    
    @DobConstraint(min = 10, message = "INVALID_DOB")
    LocalDate dob;

    @NotBlank(message = "ROLE_REQUIRED")
    @NotNull(message = "ROLE_REQUIRED")
    String role; // One of: LEARNER, CREATOR, MOD, ADMIN
}

