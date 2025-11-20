package vn.uth.firebasenotification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterTokenDto {
    private String token;
    private String platform;
    private String deviceInfo;
}

