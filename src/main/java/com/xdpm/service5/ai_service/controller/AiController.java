package com.xdpm.service5.ai_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xdpm.service5.ai_service.dto.*;
import com.xdpm.service5.ai_service.model.AiRecommendation;
import com.xdpm.service5.ai_service.service.AiRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 🤖 AiController — Controller chính cho AI Recommendation (Tuần 3)
 * Hỗ trợ RULE và RULE_LLM mode + contextData
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiRecommendationService aiService;
    private static final ObjectMapper om = new ObjectMapper();

    // -------------------------------------------------
    // POST /api/v1/ai/recommend
    // -------------------------------------------------
    @Operation(
            summary = "Tạo khuyến nghị (rule-based hoặc LLM-enhanced)",
            description = """
                - Tuần 3: hỗ trợ aiMode = RULE (mặc định) hoặc RULE_LLM
                - Có thể truyền contextData để mô phỏng dữ liệu chi tiêu gần đây
                - Dùng header Idempotency-Key để tránh ghi trùng bản ghi
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = """
                            {
                              "userId": "U001",
                              "aiMode": "RULE_LLM",
                              "contextData": {
                                "salary_month": 1000,
                                "last_30d_total": 850,
                                "by_category": {
                                  "Food": 320,
                                  "Bills": 200,
                                  "Saving": 150
                                }
                              }
                            }
                            """)
                    )
            )
    )
    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> recommend(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @RequestHeader(value = "X-AI-Mode", required = false) String headerAiMode,
            @Valid @RequestBody RecommendRequest req) throws Exception {

        String aiMode = (headerAiMode != null && !headerAiMode.isBlank())
                ? headerAiMode
                : (req.getAiMode() != null ? req.getAiMode() : "RULE");

        log.info("[AI] /recommend user={} idemKey={} aiMode={}", req.getUserId(), idemKey, aiMode);

        Map<String, Object> ctx = new HashMap<>();
        if (req.getContextData() instanceof Map<?, ?> mapCtx) {
            ctx.putAll((Map<String, Object>) mapCtx);
        }

        var resp = aiService.generateAndSave(req.getUserId(), ctx, idemKey, aiMode);

        Map<String, Object> result = Map.of(
                "user_id", resp.getUserId(),
                "ai_mode", resp.getAiMode(),
                "guard_pass", resp.isGuardPass(),
                "category", resp.getCategory(),
                "message", resp.getMessage(),
                "rules_hit", resp.getRulesHit(),
                "created_at", resp.getCreatedAt()
        );

        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------
    // GET /api/v1/ai/report
    // -------------------------------------------------
    @Operation(
            summary = "Báo cáo KPI đơn giản",
            description = "Lấy dữ liệu gần đây của user (tối đa 5 bản ghi)"
    )
    @GetMapping("/report")
    public ResponseEntity<ReportResponse> report(
            @RequestParam(value = "user_id", defaultValue = "U001") String userId) {

        log.info("[AI] /report user={}", userId);
        List<AiRecommendation> list = aiService.recentByUser(userId);

        if (CollectionUtils.isEmpty(list)) {
            return ResponseEntity.ok(ReportResponse.builder()
                    .userId(userId)
                    .kpis(List.of())
                    .build());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> kpis = list.stream().limit(5)
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "category", r.getCategory(),
                        "message", r.getMessage(),
                        "created_at", r.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(ReportResponse.builder()
                .userId(userId)
                .kpis((List<Map<String, ?>>) (List<?>) kpis) // ✅ fix lỗi type
                .build());



    }

    // -------------------------------------------------
    // GET /api/v1/ai/chart
    // -------------------------------------------------
    @Operation(
            summary = "Chart data (Chart.js format)",
            description = "Dữ liệu biểu đồ chi tiêu gần nhất của user (có cache Redis ở tuần 3)"
    )
    @GetMapping("/chart")
    public ResponseEntity<ChartResponse> chart(
            @RequestParam(value = "user_id", defaultValue = "U001") String userId) {

        log.info("[AI] /chart user={}", userId);
        List<AiRecommendation> list = aiService.recentByUser(userId);

        Map<String, Object> defaultChart = Map.of(
                "labels", List.of("Food", "Bills", "Saving"),
                "datasets", List.of(Map.of(
                        "label", "Spending vs Saving",
                        "data", List.of(45, 30, 25)
                ))
        );

        Map<String, Object> chart = (!CollectionUtils.isEmpty(list))
                ? jsonFallback(list.get(0).getChartData(), defaultChart)
                : defaultChart;

        return ResponseEntity.ok(ChartResponse.builder()
                .userId(userId)
                .data(chart)
                .build());
    }

    private Map<String, Object> jsonFallback(String json, Map<String, Object> def) {
        try {
            if (json == null || json.isBlank()) return def;
            return om.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("[AI] chart_data parse failed: {}", e.getMessage());
            return def;
        }
    }
}