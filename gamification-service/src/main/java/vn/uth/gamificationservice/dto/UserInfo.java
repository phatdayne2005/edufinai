package vn.uth.gamificationservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString
public class UserInfo {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String dob;
    private List<UserRole> roles;
}
