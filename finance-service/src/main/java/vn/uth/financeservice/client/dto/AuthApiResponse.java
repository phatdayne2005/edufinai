package vn.uth.financeservice.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthApiResponse<T> {
    private int code;
    private String message;
    private T result;
}

