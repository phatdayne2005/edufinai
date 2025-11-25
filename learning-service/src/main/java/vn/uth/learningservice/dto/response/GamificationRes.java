package vn.uth.learningservice.dto.response;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamificationRes {
    private UUID userId;
    private String sourceType; // "QUIZ"
    private UUID lessonId;
    private UUID enrollId;
    private Integer totalQuiz;
    private Integer correctAnswer;
}
