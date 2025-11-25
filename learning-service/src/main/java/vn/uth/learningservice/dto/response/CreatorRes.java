package vn.uth.learningservice.dto.response;

import lombok.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorRes {
    private UUID id;
    private String username;
    private Long totalLessons;
}