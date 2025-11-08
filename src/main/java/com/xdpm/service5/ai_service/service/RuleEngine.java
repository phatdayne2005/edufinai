package com.xdpm.service5.ai_service.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Rule-based Engine (Tuần 2)
 * Đánh giá chi tiêu, phát hiện các hành vi bất thường và sinh khuyến nghị.
 */
@Slf4j
@Service
public class RuleEngine {

    /** 🧠 Evaluate rules dựa trên FeatureBundle (dùng cho /dev/event) */
    public RuleResult evaluate(FeatureBuilder.FeatureBundle fb) {
        List<String> hitRules = new ArrayList<>();
        String suggestion = "Giữ thói quen chi tiêu lành mạnh.";
        String explanation = "Không có bất thường trong chi tiêu.";
        int score = 80;
        String category = "General";

        // Rule 1️⃣: saving_target
        if (fb.getSpendSalaryRatio().compareTo(BigDecimal.valueOf(0.8)) > 0) {
            hitRules.add("saving_target");
            category = "Saving";
            suggestion = "Nên tăng mục tiêu tiết kiệm, giảm chi tiêu xuống 15%.";
            explanation = "Chi tiêu vượt 80% thu nhập.";
            score = 90;
        }

        // Rule 2️⃣: food_overspend
        BigDecimal food = fb.getByCategory().getOrDefault("Food", BigDecimal.ZERO);
        BigDecimal total = fb.getLast30dTotal();
        if (food.divide(total, 2, BigDecimal.ROUND_HALF_UP)
                .compareTo(BigDecimal.valueOf(0.3)) > 0) {
            hitRules.add("food_overspend");
            category = "Food";
            suggestion = "Bạn đang chi tiêu quá nhiều cho ăn uống.";
            explanation = "Tỷ lệ Food > 30% tổng chi.";
            score = 85;
        }

        // Rule 3️⃣: bill_spike
        BigDecimal bills = fb.getByCategory().getOrDefault("Bills", BigDecimal.ZERO);
        if (bills.compareTo(BigDecimal.valueOf(1500000)) > 0) {
            hitRules.add("bill_spike");
            category = "Bills";
            suggestion = "Cảnh báo tăng bất thường trong hóa đơn.";
            explanation = "Bills tháng này cao hơn 1.5 lần bình thường.";
            score = 75;
        }

        RuleResult r = new RuleResult();
        r.setRuleIds(hitRules);
        r.setRulesHit(hitRules);
        r.setCategory(category);
        r.setSuggestion(suggestion);
        r.setMessage(suggestion);        // alias cho message
        r.setExplanation(explanation);
        r.setScore(score);

        log.info("rule_evaluated ruleHits={} score={} category={}", hitRules, score, category);
        return r;
    }

    /** 🧩 Overload: Evaluate rule khi đầu vào là Map (dùng cho generateAndSave) */
    @SuppressWarnings("unchecked")
    public RuleResult evaluate(Map<String, Object> feats) {
        double ratio = d(feats.get("spend_salary_ratio"));
        double salary = d(feats.get("salary_month"));
        Map<String, Object> byCat = (Map<String, Object>) feats.getOrDefault("by_category", Map.of());
        double food = d(byCat.get("Food"));
        double bills = d(byCat.get("Bills"));

        List<String> rulesHit = new ArrayList<>();
        String suggestion = "Giữ thói quen chi tiêu lành mạnh.";
        String explanation = "Không có bất thường trong chi tiêu.";
        String category = "General";
        int score = 80;

        if (ratio >= 0.8) {
            rulesHit.add("saving_target");
            category = "Saving";
            suggestion = "Nên tiết kiệm thêm 10% lương mỗi tháng.";
            explanation = "Chi tiêu vượt 80% lương.";
            score = 90;
        }

        if (food > 0.3 * salary) {
            rulesHit.add("food_overspend");
            category = "Food";
            suggestion = "Chi tiêu ăn uống vượt 30% lương, cân nhắc cắt giảm.";
            explanation = "Food > 30% salary.";
            score = 85;
        }

        if (bills > 0.25 * salary) {
            rulesHit.add("bill_spike");
            category = "Bills";
            suggestion = "Hóa đơn tháng này tăng cao, kiểm tra điện/nước/internet.";
            explanation = "Bills vượt 25% lương.";
            score = 75;
        }

        RuleResult r = new RuleResult();
        r.setRuleIds(rulesHit);
        r.setRulesHit(rulesHit);
        r.setCategory(category);
        r.setSuggestion(suggestion);
        r.setMessage(suggestion);
        r.setExplanation(explanation);
        r.setScore(score);

        log.info("rule_evaluated rulesHit={} score={}", rulesHit, score);
        return r;
    }

    /** Helper parse double */
    private double d(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0d; }
    }

    /** 🧾 Kết quả rule evaluation */
    @Data
    public static class RuleResult {
        private List<String> ruleIds;      // cho getRuleIds()
        private List<String> rulesHit;     // alias để service truy cập
        private String suggestion;         // nội dung khuyến nghị chính
        private String message;            // alias suggestion
        private String explanation;        // giải thích chi tiết
        private String category;           // nhóm khuyến nghị
        private int score;                 // điểm hoặc độ nghiêm trọng
    }
}
