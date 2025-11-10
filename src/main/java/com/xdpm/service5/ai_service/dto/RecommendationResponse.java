package com.xdpm.service5.ai_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Schema(description = "Phản hồi khi sinh khuyến nghị (recommendation hoặc event-driven)")
public class RecommendationResponse {

    @Schema(example = "21428ce3-8a82-47b7-b5ff-cd0935b61454", description = "ID khuyến nghị trong database")
    private String id;

    @Schema(example = "U001", description = "ID người dùng")
    private String userId;

    @Schema(example = "Saving", description = "Nhóm khuyến nghị: Saving / Food / Bills / General")
    private String category;

    @Schema(example = "Nên tiết kiệm thêm 10% lương mỗi tháng.", description = "Nội dung khuyến nghị")
    private String message;

    @Schema(description = "Dữ liệu đầu vào đã phân tích (features)")
    private Map<String, Object> analyzed;

    @Schema(description = "Dữ liệu biểu đồ (Chart.js structure)")
    private Map<String, Object> chart;

    @Schema(example = "2025-11-08T22:45:00.671245", description = "Thời gian tạo bản ghi")
    private LocalDateTime createdAt;

    @Schema(example = "false", description = "true nếu request/event đã bị trùng Idempotency-Key")
    private boolean idempotent;

    @Schema(example = "[\"saving_target\"]", description = "Danh sách rule đã match trong RuleEngine")
    private String[] rulesHit;

    // === Week 3 additions ===

    @Schema(example = "RULE_LLM", description = "Chế độ AI sử dụng: RULE / RULE_LLM / ML_LLM / LLM_FULL")
    private String aiMode;

    @Schema(example = "true", description = "Kết quả hậu kiểm (OutputGuard) của nội dung sinh ra")
    private boolean guardPass;

    @Schema(example = "85", description = "Điểm đánh giá mức độ khuyến nghị (từ RuleEngine)")
    private Integer score;
}
