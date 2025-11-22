package vn.uth.financeservice.client.dto;

import lombok.Data;

@Data
public class AuthUserResponse {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}

