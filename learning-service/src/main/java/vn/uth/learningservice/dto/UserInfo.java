package vn.uth.learningservice.dto;

import lombok.*;
import java.util.UUID;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {
    private UUID id;
    private String username;
    private java.util.List<Object> roles;
}
