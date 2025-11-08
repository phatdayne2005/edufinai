package com.xdpm.service5.ai_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/** Mô phỏng event từ hệ thống khác */
@Data
public class DevEventRequest {
    @NotBlank
    private String type;  // expense.created | module.quiz.submitted | salary.updated ...
    @NotBlank
    private String userId;
    private Instant occurredAt = Instant.now();
    private Map<String, Object> payload; // amount, category, salary, ...
}
