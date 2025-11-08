package com.xdpm.service5.ai_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data @Builder
public class RecommendationResponse {
    private String id;
    private String userId;
    private String category;
    private String message;
    private Map<String, Object> analyzed;
    private Map<String, Object> chart;
    private LocalDateTime createdAt;
    private boolean idempotent; // true nếu request/event trùng Idempotency-Key
    private String[] rulesHit;  // các rule khớp
}
