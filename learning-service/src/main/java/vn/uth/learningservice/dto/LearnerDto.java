package vn.uth.learningservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import vn.uth.learningservice.model.Learner;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearnerDto {

    @Size(max = 50, message = "Display name cannot exceed 50 characters.")
    private String displayName;

    // Dob (ngay thang nam sinh)
    @Past(message = "Dob must be at least one day before the current date.")
    private LocalDate dob;

    private Learner.Level level = Learner.Level.BEGINNER;

    private MultipartFile avatar;

    @Size(max = 500, message = "Bio cannot have more than 500 characters.")
    private String bio;
}
