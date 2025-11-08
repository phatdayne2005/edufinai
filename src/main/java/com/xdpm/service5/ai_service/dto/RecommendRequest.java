package com.xdpm.service5.ai_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecommendRequest {
    @NotBlank
    private String userId;
    // có thể mở rộng thêm params sau
}
