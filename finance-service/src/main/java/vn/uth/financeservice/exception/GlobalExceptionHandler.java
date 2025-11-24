package vn.uth.financeservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        
        // Determine HTTP status based on exception message
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        
        if (ex.getMessage() != null) {
            String message = ex.getMessage();
            if (message.contains("not found") || message.contains("không tồn tại")) {
                status = HttpStatus.NOT_FOUND;
            } else if (message.contains("Forbidden") || message.contains("không có quyền")) {
                status = HttpStatus.FORBIDDEN;
            } else if (message.contains("đã được khai báo") || 
                      message.contains("already exists") ||
                      message.contains("Không đủ số tiền") ||
                      message.contains("Cannot delete") ||
                      message.contains("Cannot link")) {
                status = HttpStatus.BAD_REQUEST;
            }
        }
        
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}

