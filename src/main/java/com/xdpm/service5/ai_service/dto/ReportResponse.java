package com.xdpm.service5.ai_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "Phản hồi báo cáo KPI chi tiêu (gần nhất của người dùng)")
public class ReportResponse {

    @Schema(example = "U001", description = "ID người dùng cần xem báo cáo")
    private String userId;

    @Schema(description = "Danh sách KPI gần đây, gồm nhiều loại dữ liệu khác nhau (String, Number, Date, v.v.)")
    private List<Map<String, ?>> kpis;

}
