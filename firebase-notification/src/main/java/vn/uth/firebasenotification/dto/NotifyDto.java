package vn.uth.firebasenotification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyDto {
    private String title;
    private String body;
    private Map<String, String> data;
    private String topic;
    private String token;
}

