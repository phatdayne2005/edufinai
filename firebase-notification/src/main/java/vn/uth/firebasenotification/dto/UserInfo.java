package vn.uth.firebasenotification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
}
