package vn.uth.learningservice.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import lombok.*;
import vn.uth.learningservice.dto.shared.LessonDifficulty;
import vn.uth.learningservice.dto.shared.LessonStatus;
import vn.uth.learningservice.dto.shared.LessonTag;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonUpdateReq {
    @Size(max = 150)
    private String title;

    @Size(max = 1000)
    private String description;

    private LessonStatus status;
    private LessonDifficulty difficulty;

    @Min(0)
    private Integer durationMinutes;

    @Size(max = 255)
    private String thumbnailUrl;

    @Size(max = 255)
    private String videoUrl;

    private String content;

    private Set<LessonTag> tags;

    private JsonNode quizJson;

    private String commentByMod;
}
