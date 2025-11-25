package vn.uth.gamificationservice.dto;

public class ApiResponse<T> {
    private int code;
    private T result;
    private String message;

    public ApiResponse() {
    }

    public ApiResponse(int code, T result, String message) {
        this.code = code;
        this.result = result;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

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
        return new ApiResponse<>(200, null, "");
    }

    public static <T> ApiResponse<T> empty(String message) {
        return new ApiResponse<>(200, null, message);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, null, message);
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "code=" + code +
                ", result=" + result +
                ", message='" + message + '\'' +
                '}';
    }
}
