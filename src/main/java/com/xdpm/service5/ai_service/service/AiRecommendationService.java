package com.xdpm.service5.ai_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xdpm.service5.ai_service.dto.RecommendationResponse;
import com.xdpm.service5.ai_service.model.AiRecommendation;
import com.xdpm.service5.ai_service.repository.AiRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final FeatureBuilder featureBuilder;
    private final RuleEngine ruleEngine;
    private final AiRecommendationRepository repo;
    private final IdempotencyService idem;
    private final ObjectMapper om = new ObjectMapper();

    // --------------------------------------------------------
    // 1️⃣ Generate + save recommendation (idempotent)
    // --------------------------------------------------------
    @Transactional
    public RecommendationResponse generateAndSave(String userId,
                                                  Map<String, Object> recentContext,
                                                  String idempotencyKey) throws Exception {

        boolean firstTime = true;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            firstTime = idem.tryAcquire(idempotencyKey, Duration.ofMinutes(10));
        }

        if (!firstTime) {
            var latest = repo.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                    .stream().findFirst();
            if (latest.isPresent()) {
                var rec = latest.get();
                return RecommendationResponse.builder()
                        .id(rec.getId())
                        .userId(rec.getUserId())
                        .category(rec.getCategory())
                        .message(rec.getMessage())
                        .analyzed(jsonToMap(rec.getAnalyzedData()))
                        .chart(jsonToMap(rec.getChartData()))
                        .createdAt(rec.getCreatedAt())
                        .idempotent(true)
                        .rulesHit(new String[0])
                        .build();
            }
        }

        // 1) Build features
        Map<String, Object> feats = featureBuilder.buildFeatures(userId, recentContext);

        // 2) Evaluate rules
        var rr = ruleEngine.evaluate(feats);

        // 3) Build chart
        Map<String, Object> chart = featureBuilder.buildChartData(feats);

        // 4) Persist
        String analyzedJson = om.writeValueAsString(feats);
        String chartJson = om.writeValueAsString(chart);

        AiRecommendation entity = AiRecommendation.builder()
                .userId(userId)
                .category(rr.category)
                .type(rr.category)  // khớp field type trong entity
                .message(rr.message)
                .analyzedData(analyzedJson)
                .chartData(chartJson)
                .createdAt(LocalDateTime.now())
                .build();

        repo.save(entity);

        log.info("recommendation_saved user={} id={} rules={}", userId, entity.getId(), rr.rulesHit);

        return RecommendationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .category(entity.getCategory())
                .message(entity.getMessage())
                .analyzed(feats)
                .chart(chart)
                .createdAt(entity.getCreatedAt())
                .idempotent(!firstTime)
                .rulesHit(rr.rulesHit.toArray(String[]::new))
                .build();
    }

    // --------------------------------------------------------
    // 2️⃣ Lấy bản ghi gần nhất
    // --------------------------------------------------------
    public List<AiRecommendation> recentByUser(String userId) {
        return repo.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    // --------------------------------------------------------
    // 3️⃣ Helper: JSON → Map
    // --------------------------------------------------------
    private Map<String, Object> jsonToMap(String json) throws Exception {
        if (json == null) return Map.of();
        return om.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    // --------------------------------------------------------
    // 4️⃣ Mock methods
    // --------------------------------------------------------
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
