package vn.uth.learningservice.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import vn.uth.learningservice.dto.shared.LessonDifficulty;
import vn.uth.learningservice.dto.shared.LessonTag;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonCreateReq {
    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 1000)
    private String description;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String slug;

    @NotBlank
    private String content;

    @Min(0)
    private Integer durationMinutes;

    @NotNull
    private LessonDifficulty difficulty;

    @Size(max = 255)
    private String thumbnailUrl;

    @Size(max = 255)
    private String videoUrl;

    @NotNull
    private Set<LessonTag> tags;

    // Quiz JSON: chứa thông tin số thứ tự, câu hỏi và đáp án
    private JsonNode quizJson;
}
