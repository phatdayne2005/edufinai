package com.xdpm.service5.ai_service.service;

import com.xdpm.service5.ai_service.dto.DevEventRequest;
import com.xdpm.service5.ai_service.dto.RecommendationResponse;
import com.xdpm.service5.ai_service.model.AiRecommendation;
import com.xdpm.service5.ai_service.repository.AiRecommendationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final StringRedisTemplate redisTemplate;
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

        // 🧩 Build features and apply rules
        Map<String, Object> feats = featureBuilder.buildFeatures(userId, recentContext);
        var rr = ruleEngine.evaluate(feats);
        Map<String, Object> chart = featureBuilder.buildChartData(feats);

        String coreMsg = rr.getMessage();
        String finalMsg = coreMsg;
        boolean guardPass = true;
        long nlgLatencyMs = -1;

        // 🧩 If mode = RULE_LLM → call LLM and validate output
        if (aiMode != null && aiMode.equalsIgnoreCase("RULE_LLM")) {
            long start = System.currentTimeMillis();
            String llmMsg;
            try {
                llmMsg = aiNlgService.generateText(coreMsg);
                nlgLatencyMs = System.currentTimeMillis() - start;
                guardPass = outputGuard.validate(llmMsg);
                if (guardPass) {
                    finalMsg = llmMsg;
                } else {
                    log.warn("[AI] OutputGuard failed, fallback to core message");
                }
            } catch (Exception ex) {
                nlgLatencyMs = System.currentTimeMillis() - start;
                log.error("[AI] LLM failed, fallback coreMsg, latency={}ms, error={}", nlgLatencyMs, ex.getMessage());
                finalMsg = coreMsg;
            }
        }

        // 🧩 Save recommendation
        AiRecommendation entity = AiRecommendation.builder()
                .userId(userId)
                .category(rr.getCategory())
                .type(rr.getCategory())
                .message(finalMsg)
                .analyzedData(om.writeValueAsString(feats))
                .chartData(om.writeValueAsString(chart))
                .guardPass(guardPass)
                .createdAt(LocalDateTime.now())
                .build();

        repo.saveAndFlush(entity);
        log.info("[AI] recommendation_saved user={} id={} category={} mode={} guard_pass={} latency_ms={}",
                userId, entity.getId(), rr.getCategory(), aiMode, guardPass, nlgLatencyMs);

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
                .rulesHit(rr.getRuleIds() != null
                        ? rr.getRuleIds().toArray(String[]::new)
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
        long nlgLatencyMs = -1;

        if (req.getAiMode() != null && req.getAiMode().equalsIgnoreCase("RULE_LLM")) {
            long start = System.currentTimeMillis();
            try {
                String llmMsg = aiNlgService.generateText(coreMsg);
                nlgLatencyMs = System.currentTimeMillis() - start;
                guardPass = outputGuard.validate(llmMsg);
                if (guardPass) finalMsg = llmMsg;
                else log.warn("[Event] OutputGuard rejected output - fallback to rule message");
            } catch (Exception ex) {
                nlgLatencyMs = System.currentTimeMillis() - start;
                log.error("[Event] LLM failed, fallback coreMsg, latency={}ms, error={}", nlgLatencyMs, ex.getMessage());
                finalMsg = coreMsg;
            }
        }

        AiRecommendation rec = AiRecommendation.builder()
                .userId(req.getUserId())
                .category(result.getCategory())
                .message(finalMsg)
                .analyzedData(om.writeValueAsString(features))
                .ruleHits(String.join(",", result.getRuleIds()))
                .score(result.getScore())
                .explanation(result.getExplanation())
                .guardPass(guardPass)
                .createdAt(LocalDateTime.now())
                .build();

        repo.saveAndFlush(rec);
        log.info("[Event] Processed event_type={} user={} category={} guard_pass={} latency_ms={}",
                req.getEventType(), req.getUserId(), rec.getCategory(), guardPass, nlgLatencyMs);

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

    // --------------------------------------------------------
    // 3️⃣ Aggregated KPI report (for /ai/report)
    // --------------------------------------------------------
    public Map<String, Object> getKpiReport(String userId, LocalDate from, LocalDate to) {
        var list = repo.findByUserIdAndDateBetween(userId, from.atStartOfDay(), to.atTime(23, 59));

        double avgScore = list.stream()
                .mapToDouble(r -> Optional.ofNullable(r.getScore()).orElse(0).doubleValue())
                .average()
                .orElse(0.0);


        long total = list.size();
        long guardPassCount = list.stream()
                .filter(r -> Boolean.TRUE.equals(r.getGuardPass()))
                .count();

        Map<String, Long> byCategory = list.stream()
                .collect(Collectors.groupingBy(AiRecommendation::getCategory, Collectors.counting()));

        return Map.of(
                "user_id", userId,
                "total_recommendations", total,
                "average_score", avgScore,
                "guard_passed", guardPassCount,
                "by_category", byCategory
        );
    }

    // --------------------------------------------------------
    // 4️⃣ Chart cache with TTL
    // --------------------------------------------------------
    public Map<String, Object> getChartCached(String userId) {
        String cacheKey = "chart:" + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("[CACHE HIT] chart for {}", userId);
            try {
                return om.readValue(cached, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("[CACHE PARSE ERROR] {}", e.getMessage());
            }
        }

        Map<String, Object> chart = Map.of(
                "labels", List.of("Food", "Bills", "Saving"),
                "data", List.of(45, 30, 25)
        );
        try {
            redisTemplate.opsForValue()
                    .set(cacheKey, om.writeValueAsString(chart), Duration.ofMinutes(10));
            log.info("[CACHE MISS] chart computed & cached for {}", userId);
        } catch (Exception e) {
            log.error("[CACHE SAVE ERROR] {}", e.getMessage());
        }

        return chart;
    }

    // --------------------------------------------------------
    // Utilities
    // --------------------------------------------------------
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
}
