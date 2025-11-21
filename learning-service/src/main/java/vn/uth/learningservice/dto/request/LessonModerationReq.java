package vn.uth.learningservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.uth.learningservice.model.Lesson;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonModerationReq {

    @NotNull
    private Lesson.Status status;

    @Size(max = 2000)
    private String commentByMod;
}

