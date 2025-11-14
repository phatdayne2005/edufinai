package vn.uth.edufinai.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

import vn.uth.edufinai.validator.DobConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @Size(min = 4, message = "USERNAME_INVALID")
    String username;

    @Size(min = 6, message = "INVALID_PASSWORD")
    String password;

    String firstName;
    String lastName;
    @jakarta.validation.constraints.Email(message = "INVALID_EMAIL")
    String email;

    @jakarta.validation.constraints.Size(min = 6, max = 20, message = "INVALID_PHONE")
    String phone;
    @DobConstraint(min = 10, message = "INVALID_DOB")
    LocalDate dob;
}
