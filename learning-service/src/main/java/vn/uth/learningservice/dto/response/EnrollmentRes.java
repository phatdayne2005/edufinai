package vn.uth.learningservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import vn.uth.learningservice.dto.shared.EnrollmentStatus;

import java.time.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentRes {
    private UUID id;
    private UUID learnerId;
    private UUID lessonId;
    private EnrollmentStatus status;
    private int progressPercent;
    private Integer score;
    private int attempts;
    private Integer correctAnswers;
    private Long earnedExp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActivityAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
