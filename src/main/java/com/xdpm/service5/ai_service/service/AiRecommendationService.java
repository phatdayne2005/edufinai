package com.xdpm.service5.ai_service.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;

@Service
public class AiRecommendationService {

    public Map<String, Object> generateMockRecommendation(String userId) {
        Map<String, Object> res = new HashMap<>();
        res.put("user_id", userId);
        res.put("suggestion", "Nên tiết kiệm thêm 10% lương mỗi tháng.");
        res.put("action", "SET_GOAL");
        res.put("score", 88);
        return res;
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