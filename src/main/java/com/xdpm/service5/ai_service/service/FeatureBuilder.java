package com.xdpm.service5.ai_service.service;

import com.xdpm.service5.ai_service.dto.DevEventRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 🧩 FeatureBuilder — xây dựng các đặc trưng chi tiêu/thu nhập
 * dùng cho rule-based AI và event-driven ingestion (Tuần 2).
 */
@Slf4j
@Service
public class FeatureBuilder {

    /**
     * 🔹 Build feature từ context (dành cho /api/v1/ai/recommend)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> buildFeatures(String userId, Map<String, Object> recentContext) {
        double salaryMonth = asDouble(recentContext.getOrDefault("salary_month", 1500));
        double last30dTotal = asDouble(recentContext.getOrDefault("last_30d_total", 1200));

        // ✅ Ưu tiên đọc từ "by_category" nếu có
        Map<String, Object> byCategory = (Map<String, Object>) recentContext.get("by_category");
        if (byCategory == null || byCategory.isEmpty()) {
            // fallback sang các key cũ
            double food = asDouble(recentContext.getOrDefault("food_spend", 450));
            double bills = asDouble(recentContext.getOrDefault("bills_spend", 300));
            double saving = Math.max(0, salaryMonth - last30dTotal);
            byCategory = Map.of(
                    "Food", food,
                    "Bills", bills,
                    "Saving", saving
            );
        }

        double ratioSpend = (salaryMonth > 0) ? last30dTotal / salaryMonth : 0.0;

        Map<String, Object> feats = new LinkedHashMap<>();
        feats.put("user_id", userId);
        feats.put("date", LocalDate.now().toString());
        feats.put("salary_month", salaryMonth);
        feats.put("last_30d_total", last30dTotal);
        feats.put("spend_salary_ratio", ratioSpend);
        feats.put("by_category", byCategory);

        log.info("[FeatureBuilder] Built features user={} ratio={} by_category={}", userId, ratioSpend, byCategory);
        return feats;
    }


    /**
     * 📊 Dữ liệu cho Chart.js (trả về dạng phổ biến {labels, datasets})
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> buildChartData(Map<String, Object> feats) {
        Map<String, Object> byCat = (Map<String, Object>) feats.getOrDefault("by_category", Map.of());
        List<String> labels = new ArrayList<>(byCat.keySet());
        List<Object> data = labels.stream().map(byCat::get).toList();

        return Map.of(
                "labels", labels,
                "datasets", List.of(Map.of(
                        "label", "Spending vs Saving",
                        "data", data
                ))
        );
    }

    /**
     * 🧠 Xây dựng feature bundle từ event request (/dev/event)
     */
    public FeatureBundle fromEvent(DevEventRequest req) {
        Map<String, Object> p = Optional.ofNullable(req.getPayload()).orElse(Map.of());

        BigDecimal salaryMonth = bd(p.getOrDefault("salary_month", 1500));
        BigDecimal last30dTotal = bd(p.getOrDefault("last_30d_total", 1200));
        BigDecimal food = bd(p.getOrDefault("food_spend", 450));
        BigDecimal bills = bd(p.getOrDefault("bills_spend", 300));
        BigDecimal saving = salaryMonth.subtract(last30dTotal).max(BigDecimal.ZERO);

        BigDecimal ratio = (salaryMonth.compareTo(BigDecimal.ZERO) > 0)
                ? last30dTotal.divide(salaryMonth, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, BigDecimal> byCat = new LinkedHashMap<>();
        byCat.put("Food", food);
        byCat.put("Bills", bills);
        byCat.put("Saving", saving);

        FeatureBundle fb = new FeatureBundle();
        fb.setSpendSalaryRatio(ratio);
        fb.setLast30dTotal(last30dTotal);
        fb.setSalaryMonth(salaryMonth);
        fb.setByCategory(byCat);

        log.info("[FeatureBuilder] Event={} ratio={} user={}", req.getEventType(), ratio, req.getUserId());
        return fb;
    }

    // --------------------------------------------------------
    // 🔧 Helper methods
    // --------------------------------------------------------

    private double asDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return 0d;
        }
    }

    private BigDecimal bd(Object o) {
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(o));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // --------------------------------------------------------
    // 📦 Feature bundle cho RuleEngine
    // --------------------------------------------------------
    @Data
    public static class FeatureBundle {
        private BigDecimal spendSalaryRatio;
        private BigDecimal last30dTotal;
        private BigDecimal salaryMonth;
        private Map<String, BigDecimal> byCategory;
    }
}
