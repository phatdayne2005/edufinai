package vn.uth.learningservice.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import vn.uth.learningservice.dto.shared.LessonDifficulty;
import vn.uth.learningservice.dto.shared.LessonStatus;
import vn.uth.learningservice.dto.shared.LessonTag;

import java.time.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonRes {
    private UUID id;
    private String title;
    private String description;
    private String slug;
    private String content;
    private LessonStatus status;
    private LessonDifficulty difficulty;
    private Integer durationMinutes;
    private Set<LessonTag> tags;
    private String thumbnailUrl;
    private String videoUrl;
    private String commentByMod;
    private JsonNode quizJson;
    private Integer totalQuestions;
    private UUID creatorId;
    private UUID moderatorId;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant publishedAt;
}
