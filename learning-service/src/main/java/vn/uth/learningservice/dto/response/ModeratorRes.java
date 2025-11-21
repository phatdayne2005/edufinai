package vn.uth.learningservice.dto.response;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ModeratorRes {
    private UUID id;
    @Size(max = 500)
    private String comment;
    private Long pendingAssigned; // tuỳ controller có muốn trả hay không
}
