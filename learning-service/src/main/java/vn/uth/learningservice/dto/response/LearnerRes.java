package vn.uth.learningservice.dto.response;

import lombok.*;
import vn.uth.learningservice.dto.shared.LearnerLevel;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearnerRes {
    private UUID id;
    private LearnerLevel level;
    private Long totalExp;
    private Integer expPercent; // 0-100%
}