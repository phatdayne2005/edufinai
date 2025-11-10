package com.xdpm.service5.ai_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@Schema(description = "Phản hồi chứa dữ liệu biểu đồ (Chart.js format)")
public class ChartResponse {

    @Schema(example = "U001", description = "ID người dùng")
    private String userId;

    @Schema(description = "Cấu trúc dữ liệu biểu đồ (labels, datasets)")
    private Map<String, Object> data;
}
