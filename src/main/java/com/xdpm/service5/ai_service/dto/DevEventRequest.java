package com.xdpm.service5.ai_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Schema(description = "Yêu cầu mô phỏng sự kiện từ hệ thống khác (expense, salary, quiz, ...). Tuần 3: hỗ trợ aiMode.")
public class DevEventRequest {

    @NotBlank
    @Schema(example = "E1001", description = "ID duy nhất của sự kiện")
    private String eventId;

    @NotBlank
    @Schema(example = "salary.updated", description = "Loại sự kiện (expense.created | salary.updated ...)")
    private String eventType;

    @NotBlank
    @Schema(example = "U001", description = "ID người dùng liên quan đến sự kiện")
    private String userId;

    @Schema(example = "2025-11-08T18:00:00Z", description = "Thời điểm xảy ra sự kiện (UTC)")
    private Instant occurredAt = Instant.now();

    @Schema(example = "{\"salary_month\":1500, \"last_30d_total\":1200, \"food_spend\":450, \"bills_spend\":300}",
            description = "Dữ liệu chi tiết của sự kiện (tùy theo loại)")
    private Map<String, Object> payload;

    // ✅ Tuần 3: Bổ sung chế độ AI
    @Schema(example = "RULE_LLM", description = "Chế độ AI xử lý event: RULE (mặc định) hoặc RULE_LLM")
    private String aiMode;
}
