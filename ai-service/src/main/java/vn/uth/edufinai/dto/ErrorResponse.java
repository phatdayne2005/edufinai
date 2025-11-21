package vn.uth.edufinai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@SuperBuilder
public class ErrorResponse {
    private String code;
    private String message;

    /** Mặc định thời điểm tạo đối tượng */
    private ZonedDateTime timestamp = ZonedDateTime.now();

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = ZonedDateTime.now();
    }
}

