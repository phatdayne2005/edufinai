package com.xdpm.service5.ai_service.controller;

import com.xdpm.service5.ai_service.dto.DevEventRequest;
import com.xdpm.service5.ai_service.dto.RecommendationResponse;
import com.xdpm.service5.ai_service.service.AiRecommendationService;
import com.xdpm.service5.ai_service.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/dev")
public class DevEventController {

    private final IdempotencyService idem;
    private final AiRecommendationService aiService;

    @Operation(summary = "Mô phỏng event (expense.created, module.quiz.submitted, salary.updated, ...)",
            description = "Dùng header Idempotency-Key để đảm bảo event lặp không tạo record mới. " +
                    "payload có thể chứa: amount, category, salary_month, food_spend, bills_spend, last_30d_total ...")
    @PostMapping("/event")
    public ResponseEntity<?> emitEvent(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @Valid @RequestBody DevEventRequest req) throws Exception {

        // Nếu không có key → tự sinh key đơn giản từ type+occurredAt+user
        String key = (idemKey == null || idemKey.isBlank())
                ? ("evt:" + req.getType() + ":" + req.getUserId() + ":" + req.getOccurredAt().toEpochMilli())
                : idemKey;

        boolean acquired = idem.tryAcquire(key, Duration.ofMinutes(10));
        if (!acquired) {
            log.info("event_duplicate key={} type={} user={}", key, req.getType(), req.getUserId());
            return ResponseEntity.ok(Map.of(
                    "status", "duplicate_ignored",
                    "idempotencyKey", key
            ));
        }

        // “Ingest event → build features → rule → save”
        RecommendationResponse resp = aiService.generateAndSave(
                req.getUserId(),
                req.getPayload() == null ? Map.of() : req.getPayload(),
                key // dùng chung key cho recommend
        );

        return ResponseEntity.ok(Map.of(
                "status", "processed",
                "idempotencyKey", key,
                "recommendation", resp
        ));
    }
}
