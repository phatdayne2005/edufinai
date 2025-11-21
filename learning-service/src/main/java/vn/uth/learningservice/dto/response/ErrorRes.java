package vn.uth.learningservice.dto.response;

import lombok.*;
import java.util.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ErrorRes {
    private String code; // "NOT_FOUND", "VALIDATION", "FORBIDDEN"...
    private String message;
    private Map<String, String> fieldErrors;
}