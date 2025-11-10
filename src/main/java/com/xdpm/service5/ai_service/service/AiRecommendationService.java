package com.xdpm.service5.ai_service.service;

import com.xdpm.service5.ai_service.dto.DevEventRequest;
import com.xdpm.service5.ai_service.dto.RecommendationResponse;
import com.xdpm.service5.ai_service.model.AiRecommendation;
import com.xdpm.service5.ai_service.repository.AiRecommendationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final FeatureBuilder featureBuilder;
    private final RuleEngine ruleEngine;
    private final AiRecommendationRepository repo;
    private final IdempotencyService idem;
    private final AiNlgService aiNlgService;
    private final OutputGuard outputGuard;
    private final ObjectMapper om = new ObjectMapper();

    // --------------------------------------------------------
    // 1️⃣ Generate + save recommendation (idempotent + RULE_LLM)
    // --------------------------------------------------------
    @Transactional
    public RecommendationResponse generateAndSave(
            String userId,
            Map<String, Object> recentContext,
            String idempotencyKey,
            String aiMode) throws Exception {

        boolean firstTime = true;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            firstTime = idem.tryAcquire(idempotencyKey, Duration.ofMinutes(10));
        }

        if (!firstTime) {
            return repo.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .findFirst()
                    .map(rec -> RecommendationResponse.builder()
                            .id(rec.getId())
                            .userId(rec.getUserId())
                            .category(rec.getCategory())
                            .message(rec.getMessage())
                            .analyzed(jsonToMapSafe(rec.getAnalyzedData()))
                            .chart(jsonToMapSafe(rec.getChartData()))
                            .createdAt(rec.getCreatedAt())
                            .idempotent(true)
                            .rulesHit(new String[0])
                            .aiMode(aiMode)
                            .build())
                    .orElseGet(() -> RecommendationResponse.builder()
                            .userId(userId)
                            .message("Không có bản ghi trước đó.")
                            .idempotent(true)
                            .aiMode(aiMode)
                            .build());
        }

        Map<String, Object> feats = featureBuilder.buildFeatures(userId, recentContext);
        var rr = ruleEngine.evaluate(feats);
        Map<String, Object> chart = featureBuilder.buildChartData(feats);

        String coreMsg = rr.getMessage();
        String finalMsg = coreMsg;
        boolean guardPass = true;

        if (aiMode != null && aiMode.equalsIgnoreCase("RULE_LLM")) {
            String llmMsg = aiNlgService.generateText(coreMsg);
            guardPass = outputGuard.validate(llmMsg);
            if (guardPass) {
                finalMsg = llmMsg;
            } else {
                log.warn("[AI] OutputGuard failed, fallback to core message");
            }
        }

        AiRecommendation entity = AiRecommendation.builder()
                .userId(userId)
                .category(rr.getCategory())
                .type(rr.getCategory())
                .message(finalMsg)
                .analyzedData(om.writeValueAsString(feats))
                .chartData(om.writeValueAsString(chart))
                .createdAt(LocalDateTime.now())
                .build();

        repo.saveAndFlush(entity);
        log.info("[AI] recommendation_saved user={} id={} category={} mode={} rules={} guard_pass={} latency_ms=NA",
                userId, entity.getId(), rr.getCategory(), aiMode, rr.getRulesHit(), guardPass);

        return RecommendationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .category(entity.getCategory())
                .message(entity.getMessage())
                .analyzed(feats)
                .chart(chart)
                .createdAt(entity.getCreatedAt())
                .idempotent(!firstTime)
                .aiMode(aiMode)
                .guardPass(guardPass)
                .rulesHit(rr.getRulesHit() != null
                        ? rr.getRulesHit().toArray(String[]::new)
                        : new String[0])
                .build();
    }

    // --------------------------------------------------------
    // 2️⃣ Handle DevEvent (Event-driven)
    // --------------------------------------------------------
    @Transactional
    public RecommendationResponse ingestEvent(DevEventRequest req) throws Exception {
        log.info("[Event] Received type={} id={} user={}", req.getEventType(), req.getEventId(), req.getUserId());

        var features = featureBuilder.fromEvent(req);
        var result = ruleEngine.evaluate(features);

        String coreMsg = result.getSuggestion();
        String finalMsg = coreMsg;
        boolean guardPass = true;

        if (req.getAiMode() != null && req.getAiMode().equalsIgnoreCase("RULE_LLM")) {
            String llmMsg = aiNlgService.generateText(coreMsg);
            guardPass = outputGuard.validate(llmMsg);
            if (guardPass) finalMsg = llmMsg;
            else log.warn("[Event] OutputGuard rejected output - fallback to rule message");
        }

        AiRecommendation rec = AiRecommendation.builder()
                .userId(req.getUserId())
                .category(result.getCategory())
                .message(finalMsg)
                .analyzedData(om.writeValueAsString(features))
                .ruleHits(String.join(",", result.getRuleIds()))
                .score(result.getScore())
                .explanation(result.getExplanation())
                .createdAt(LocalDateTime.now())
                .build();

        repo.saveAndFlush(rec);

        log.info("[Event] Processed event_type={} user={} category={} rules={} guard_pass={}",
                req.getEventType(), req.getUserId(), rec.getCategory(), result.getRuleIds(), guardPass);

        return RecommendationResponse.builder()
                .id(rec.getId())
                .userId(rec.getUserId())
                .category(rec.getCategory())
                .message(rec.getMessage())
                .analyzed(Map.of(
                        "salary_month", features.getSalaryMonth(),
                        "spend_salary_ratio", features.getSpendSalaryRatio(),
                        "by_category", features.getByCategory()
                ))
                .chart(Map.of())
                .createdAt(rec.getCreatedAt())
                .idempotent(false)
                .aiMode(req.getAiMode())
                .guardPass(guardPass)
                .rulesHit(result.getRuleIds().toArray(String[]::new))
                .build();
    }

    public List<AiRecommendation> recentByUser(String userId) {
        return repo.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    private Map<String, Object> jsonToMapSafe(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return om.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[AI] jsonToMap failed: {}", e.getMessage());
            return Map.of();
        }
    }

    // Mock methods retained for backward compatibility
    public Map<String, Object> generateMockRecommendation(String userId) {
        AiRecommendation rec = AiRecommendation.builder()
                .userId(userId)
                .category("Saving")
                .type("Saving")
                .message("Nên tiết kiệm thêm 10% lương mỗi tháng.")
                .analyzedData("{\"spending_ratio\":0.8}")
                .chartData("{\"labels\":[\"Food\",\"Bills\",\"Saving\"],\"data\":[45,30,25]}")
                .createdAt(LocalDateTime.now())
                .build();

        repo.save(rec);

        return Map.of(
                "id", rec.getId(),
                "user_id", rec.getUserId(),
                "category", rec.getCategory(),
                "message", rec.getMessage(),
                "action", "SET_GOAL",
                "score", 88
        );
    }

    public Map<String, Object> getMockReport(String userId) {
        return Map.of(
                "user_id", userId,
                "spending_summary", Map.of("Food", 45, "Bills", 30, "Saving", 25),
                "saving_rate", "25%"
        );
    }

    public Map<String, Object> getMockChart(String userId) {
        return Map.of(
                "labels", new String[]{"Food", "Bills", "Saving"},
                "data", new int[]{45, 30, 25}
        );
    }
}