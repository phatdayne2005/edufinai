package com.xdpm.service5.ai_service.service;

import com.xdpm.service5.ai_service.dto.DevEventRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;

/**
 * Xây dựng các feature phân tích chi tiêu / thu nhập
 * từ payload hoặc event (mock dữ liệu tuần 2).
 */
@Slf4j
@Service
public class FeatureBuilder {

    /** 🧩 build các feature đơn giản từ "sự kiện" đã nhận (ở tuần 2 mock dữ liệu) */
    public Map<String, Object> buildFeatures(String userId, Map<String, Object> recentContext) {
        // Mock/giả lập: lấy từ payload gần nhất nếu có, fallback giá trị demo
        double salaryMonth = asDouble(recentContext.getOrDefault("salary_month", 1500));
        double last30dTotal = asDouble(recentContext.getOrDefault("last_30d_total", 1200));
        double food = asDouble(recentContext.getOrDefault("food_spend", 450));
        double bills = asDouble(recentContext.getOrDefault("bills_spend", 300));
        double saving = Math.max(0, salaryMonth - last30dTotal);

        double ratioSpend = last30dTotal / Math.max(1, salaryMonth); // 0.0 ~ 2.0
        Map<String, Object> byCategory = Map.of(
                "Food", food, "Bills", bills, "Saving", saving
        );

        Map<String, Object> feats = new LinkedHashMap<>();
        feats.put("user_id", userId);
        feats.put("date", LocalDate.now().toString());
        feats.put("salary_month", salaryMonth);
        feats.put("last_30d_total", last30dTotal);
        feats.put("spend_salary_ratio", ratioSpend);
        feats.put("by_category", byCategory);

        log.info("features_built user={} ratio={} last30d={} byCat={}", userId, ratioSpend, last30dTotal, byCategory);
        return feats;
    }

    private double asDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0d; }
    }

    /** 📊 data cho Chart.js (dạng phổ biến) */
    public Map<String, Object> buildChartData(Map<String, Object> feats) {
        Map<String, Object> byCat = (Map<String, Object>) feats.getOrDefault("by_category", Map.of());
        List<String> labels = new ArrayList<>(byCat.keySet());
        List<Object> data = labels.stream().map(byCat::get).toList();

        return Map.of(
                "labels", labels,
                "datasets", List.of(Map.of("label", "Spending vs Saving", "data", data))
        );
    }

    /** 🧠 từ DevEventRequest (dành cho ingestEvent) */
    public FeatureBundle fromEvent(DevEventRequest req) {
        Map<String, Object> p = Optional.ofNullable(req.getPayload()).orElse(Map.of());

        BigDecimal salaryMonth = BigDecimal.valueOf(
                ((Number) p.getOrDefault("salary_month", 1500)).doubleValue());
        BigDecimal last30dTotal = BigDecimal.valueOf(
                ((Number) p.getOrDefault("last_30d_total", 1200)).doubleValue());
        BigDecimal food = BigDecimal.valueOf(
                ((Number) p.getOrDefault("food_spend", 450)).doubleValue());
        BigDecimal bills = BigDecimal.valueOf(
                ((Number) p.getOrDefault("bills_spend", 300)).doubleValue());
        BigDecimal saving = salaryMonth.subtract(last30dTotal).max(BigDecimal.ZERO);

        BigDecimal ratio = last30dTotal.divide(salaryMonth, 2, BigDecimal.ROUND_HALF_UP);

        Map<String, BigDecimal> byCat = new LinkedHashMap<>();
        byCat.put("Food", food);
        byCat.put("Bills", bills);
        byCat.put("Saving", saving);

        FeatureBundle fb = new FeatureBundle();
        fb.setSpendSalaryRatio(ratio);
        fb.setLast30dTotal(last30dTotal);
        fb.setSalaryMonth(String.valueOf(salaryMonth));
        fb.setByCategory(byCat);

        log.info("[FeatureBuilder] fromEvent={} ratio={} -> {}", req.getEventType(), ratio, fb);
        return fb;
    }

    /** 💡 Nhóm các feature để RuleEngine sử dụng */
    @Data
    public static class FeatureBundle {
        private BigDecimal spendSalaryRatio;
        private BigDecimal last30dTotal;
        private String salaryMonth;
        private Map<String, BigDecimal> byCategory;
    }
}
