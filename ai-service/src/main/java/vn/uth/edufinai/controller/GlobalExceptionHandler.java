package vn.uth.edufinai.controller;

import vn.uth.edufinai.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
    private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, ServerWebInputException.class})
    public Mono<ErrorResponse> handleValidation(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Validation failed";
        log.warn("Validation error: {}", message);
        return Mono.just(new ErrorResponse(VALIDATION_ERROR_CODE, message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        int statusCode = ex.getStatusCode().value();
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        log.debug("Response status exception: {} - {}", statusCode, message);
        return Mono.just(new ErrorResponse(String.valueOf(statusCode), message));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ErrorResponse> handleAll(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Internal server error";
        log.error("Unexpected error: {}", message, ex);
        return Mono.just(new ErrorResponse(INTERNAL_ERROR_CODE, message));
    }
}
