package vn.uth.learningservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import vn.uth.learningservice.dto.shared.EnrollmentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentProgressReq {
    @NotNull
    private EnrollmentStatus status;

    @Min(0)
    @Max(100)
    private int progressPercent;

    @Min(0)
    private Integer score; // null = giữ nguyên

    @Min(0)
    private int addAttempt;

    @Min(0)
    private Integer correctAnswers;
}
