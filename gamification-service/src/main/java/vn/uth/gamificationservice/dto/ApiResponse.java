package vn.uth.gamificationservice.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private T result;
    private String message;

    public static <T> ApiResponse<T> empty() {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setResult(null);
        response.setMessage("");
        return response;
    }
}
