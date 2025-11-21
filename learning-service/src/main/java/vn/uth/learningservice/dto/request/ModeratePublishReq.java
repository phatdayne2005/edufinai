package vn.uth.learningservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import vn.uth.learningservice.dto.shared.LessonStatus;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ModeratePublishReq {
    @NotNull
    private LessonStatus status;
    private boolean publishNow;
}