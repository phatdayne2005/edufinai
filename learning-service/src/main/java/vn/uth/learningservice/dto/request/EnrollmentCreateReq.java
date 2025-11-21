package vn.uth.learningservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentCreateReq {
    @NotNull
    private UUID lessonId; // learnerId lấy từ JWT
}
