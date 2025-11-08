package com.xdpm.service5.ai_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data @Builder
public class ChartResponse {
    private String userId;
    private Map<String, Object> data; // Chart.js structure
}
