package com.xdpm.service5.ai_service.controller;

import com.xdpm.service5.ai_service.dto.DevEventRequest;
import com.xdpm.service5.ai_service.dto.RecommendationResponse;
import com.xdpm.service5.ai_service.service.AiRecommendationService;
import com.xdpm.service5.ai_service.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * 🔄 DevEventController — Mô phỏng event-driven flow (Tuần 3)
 * Bổ sung hỗ trợ aiMode (RULE | RULE_LLM)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/dev")
public class DevEventController {

    private final IdempotencyService idem;
    private final AiRecommendationService aiService;

    @Operation(
            summary = "Mô phỏng event (expense.created, salary.updated, ...) — hỗ trợ RULE_LLM mode",
            description = """
                - Dùng header **Idempotency-Key** để đảm bảo event lặp không tạo record mới.
                - Dùng header tùy chọn **X-AI-Mode** để chọn chế độ: RULE (default) / RULE_LLM.
                - Payload có thể chứa: `salary_month`, `last_30d_total`, `food_spend`, `bills_spend`, ...
                - Dành cho kiểm thử event-driven ở Tuần 3.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(value = """
                            {
                              "eventId": "E1001",
                              "eventType": "expense.created",
                              "userId": "U001",
                              "aiMode": "RULE_LLM",
                              "payload": {
                                "salary_month": 1200,
                                "last_30d_total": 1100,
                                "food_spend": 450,
                                "bills_spend": 280
                              }
                            }
                            """)
                    )
            )
    )
    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> emitEvent(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @RequestHeader(value = "X-AI-Mode", required = false) String headerAiMode,
            @Valid @RequestBody DevEventRequest req) throws Exception {

        // 🧠 1️⃣ Xác định aiMode (ưu tiên header)
        String aiMode = (headerAiMode != null && !headerAiMode.isBlank())
                ? headerAiMode
                : (req.getAiMode() != null ? req.getAiMode() : "RULE");

        // 🧱 2️⃣ Sinh Idempotency Key
        String key = (idemKey == null || idemKey.isBlank())
                ? ("evt:" + req.getUserId() + ":" + req.getEventType())
                : idemKey;

        log.info("[Event] Received event={} user={} aiMode={} key={}",
                req.getEventType(), req.getUserId(), aiMode, key);

        // 🚦 3️⃣ Kiểm tra trùng
        boolean acquired = idem.tryAcquire(key, Duration.ofMinutes(10));
        if (!acquired) {
            log.warn("[Event] Duplicate ignored key={} type={} user={}", key, req.getEventType(), req.getUserId());
            return ResponseEntity.ok(Map.of(
                    "status", "duplicate_ignored",
                    "idempotencyKey", key
            ));
        }

        // ⚙️ 4️⃣ Gọi service xử lý recommendation
        req.setAiMode(aiMode); // gắn aiMode vào request
        RecommendationResponse resp = aiService.ingestEvent(req);

        // 📦 5️⃣ Trả response chi tiết hơn (tuần 3 có guardPass + aiMode)
        Map<String, Object> result = Map.of(
                "status", "processed",
                "aiMode", resp.getAiMode(),
                "guardPass", resp.isGuardPass(),
                "idempotencyKey", key,
                "category", resp.getCategory(),
                "message", resp.getMessage(),
                "rulesHit", resp.getRulesHit()
        );

        log.info("[Event] Processed type={} user={} category={} aiMode={} rules={}",
                req.getEventType(), req.getUserId(), resp.getCategory(), aiMode, resp.getRulesHit());

        return ResponseEntity.ok(result);
    }
}