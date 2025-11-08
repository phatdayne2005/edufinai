package com.xdpm.service5.ai_service.controller;

import com.xdpm.service5.ai_service.dto.*;
import com.xdpm.service5.ai_service.model.AiRecommendation;
import com.xdpm.service5.ai_service.service.AiRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiRecommendationService aiService;

    // -------------------------------------------------
    // POST /api/v1/ai/recommend
    // -------------------------------------------------
    @Operation(summary = "Tạo khuyến nghị (rule-based) và ghi DB",
            description = "Dùng Idempotency-Key để tránh tạo bản ghi trùng khi gọi lại.")
    @PostMapping("/recommend")
    public ResponseEntity<RecommendationResponse> recommend(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @Valid @RequestBody RecommendRequest req) throws Exception {

        Map<String, Object> ctx = Map.of(); // mock context (tuần 2: chưa có event store)
        var resp = aiService.generateAndSave(req.getUserId(), ctx, idemKey);
        return ResponseEntity.ok(resp);
    }

    // -------------------------------------------------
    // GET /api/v1/ai/report
    // -------------------------------------------------
    @Operation(summary = "Báo cáo KPI đơn giản", description = "Lấy dữ liệu gần đây của user")
    @GetMapping("/report")
    public ResponseEntity<ReportResponse> report(@RequestParam("user_id") String userId) {
        List<AiRecommendation> list = aiService.recentByUser(userId);

        // dùng HashMap để tránh lỗi generic của Map.of(...)
        List<Map<String, Object>> kpis = list.stream().limit(5)
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("category", r.getCategory());
                    map.put("message", r.getMessage());
                    map.put("created_at", r.getCreatedAt());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(ReportResponse.builder()
                .userId(userId)
                .kpis(kpis)
                .build());
    }

    // -------------------------------------------------
    // GET /api/v1/ai/chart
    // -------------------------------------------------
    @Operation(summary = "Chart data (Chart.js format)")
    @GetMapping("/chart")
    public ResponseEntity<ChartResponse> chart(@RequestParam("user_id") String userId) {
        List<AiRecommendation> list = aiService.recentByUser(userId);
        Map<String, Object> chart = Map.of(
                "labels", List.of("Food", "Bills", "Saving"),
                "datasets", List.of(Map.of("label", "Last", "data", List.of(45, 30, 25)))
        );
        if (!list.isEmpty()) {
            chart = jsonFallback(list.get(0).getChartData(), chart);
        }
        return ResponseEntity.ok(ChartResponse.builder()
                .userId(userId)
                .data(chart)
                .build());
    }

    // Helper nhỏ để đọc JSON chart_data
    private Map<String, Object> jsonFallback(String json, Map<String, Object> def) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return def;
        }
    }
}
