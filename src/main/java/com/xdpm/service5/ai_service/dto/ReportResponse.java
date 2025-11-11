package com.xdpm.service5.ai_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Phản hồi báo cáo KPI chi tiêu và AI recommendation của người dùng")
public class ReportResponse {

    @Schema(example = "U001", description = "ID người dùng cần xem báo cáo")
    private String userId;

    // ✅ KPI chính — dùng cho thống kê tổng hợp
    @Schema(description = "Tổng số khuyến nghị trong khoảng thời gian đã chọn")
    private long totalRecommendations;

    @Schema(description = "Điểm trung bình (score) của các khuyến nghị đã lưu")
    private double averageScore;

    @Schema(description = "Số khuyến nghị vượt kiểm tra an toàn (guard_pass)")
    private long guardPassCount;

    @Schema(description = "Thống kê số khuyến nghị theo từng danh mục (Saving, Food, Bill, ...)")
    private Map<String, Long> byCategory;

    // ✅ Giữ lại trường kpis cũ để tương thích ngược nếu cần
    @Schema(description = "Danh sách KPI bổ sung (tùy mục đích hiển thị)")
    private List<Map<String, ?>> kpis;
}
