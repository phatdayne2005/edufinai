package com.xdpm.service5.ai_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Yêu cầu tạo khuyến nghị mới (Rule-based hoặc LLM-enhanced Recommendation)")
public class RecommendRequest {

    @NotBlank
    @Schema(example = "U001", description = "ID người dùng cần tạo khuyến nghị")
    private String userId;

    @Schema(example = "RULE_LLM", description = "Chế độ AI: RULE / RULE_LLM / ML_LLM / LLM_FULL (mặc định RULE)")
    private String aiMode;

    @Schema(description = "Loại dữ liệu được phân tích: EXPENSE / EVENT / LEARNING / GENERAL")
    private String dataType;

    @Schema(description = "Dữ liệu ngữ cảnh gần đây hoặc thông tin đầu vào tuỳ theo ai_mode")
    private Object contextData;
}
