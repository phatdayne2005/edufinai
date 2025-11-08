package com.xdpm.service5.ai_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data @Builder
public class ReportResponse {
    private String userId;
    private List<Map<String, Object>> kpis; // ví dụ tổng 30 ngày, tỉ lệ chi tiêu, ...
}
