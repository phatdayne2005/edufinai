package com.xdpm.service5.ai_service.service;

import com.xdpm.service5.ai_service.model.AiRecommendation;
import com.xdpm.service5.ai_service.repository.AiRecommendationRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AiRecommendationService {

    private final AiRecommendationRepository repo;

    public AiRecommendationService(AiRecommendationRepository repo) {
        this.repo = repo;
    }

    public Map<String, Object> generateMockRecommendation(String userId) {
        AiRecommendation rec = new AiRecommendation();
        rec.setAiId(UUID.randomUUID().toString());
        rec.setUserId(userId);
        rec.setType("Saving");
        rec.setSuggestion("Nên tiết kiệm thêm 10% lương mỗi tháng.");
        rec.setAnalyzedData("{\"spending_ratio\":0.8}");
        rec.setChartData("{\"labels\":[\"Food\",\"Bills\",\"Saving\"],\"data\":[45,30,25]}");
        rec.setCreatedAt(LocalDateTime.now());
        repo.save(rec);

        return Map.of(
                "ai_id", rec.getAiId(),
                "user_id", userId,
                "suggestion", rec.getSuggestion(),
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
