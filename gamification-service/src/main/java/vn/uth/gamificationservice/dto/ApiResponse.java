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

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, data, "SUCCESS");
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, data, message);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(200, null, message);
    }

    public static <T> ApiResponse<T> empty() {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setResult(null);
        response.setMessage("");
        return response;
    }

    public static <T> ApiResponse<T> empty(String message) {
        return new ApiResponse<>(200, null, message);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, null, message);
    }
}
