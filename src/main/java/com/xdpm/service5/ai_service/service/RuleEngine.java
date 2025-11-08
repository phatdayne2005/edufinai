package com.xdpm.service5.ai_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class RuleEngine {

    public static class RuleResult {
        public final List<String> rulesHit = new ArrayList<>();
        public String category = "General";
        public String message = "Giữ thói quen chi tiêu lành mạnh.";

        public RuleResult add(String ruleId) { rulesHit.add(ruleId); return this; }
    }

    @SuppressWarnings("unchecked")
    public RuleResult evaluate(Map<String, Object> feats) {
        double ratio = d(feats.get("spend_salary_ratio"));      // tỉ lệ chi / lương
        double salary = d(feats.get("salary_month"));
        Map<String,Object> byCat = (Map<String, Object>) feats.getOrDefault("by_category", Map.of());
        double food = d(byCat.get("Food"));
        double bills = d(byCat.get("Bills"));

        RuleResult r = new RuleResult();

        // Rule #1: saving_target – nếu ratio > 0.8 ⇒ khuyên tiết kiệm thêm 10%
        if (ratio > 0.8) {
            r.add("saving_target");
            r.category = "Saving";
            r.message = "Nên tiết kiệm thêm 10% lương mỗi tháng.";
        }

        // Rule #2: food_overspend – nếu Food > 30% lương
        if (food > 0.3 * salary) {
            r.add("food_overspend");
            r.category = "Food";
            r.message = "Chi tiêu ăn uống vượt 30% lương, cân nhắc cắt giảm.";
        }

        // Rule #3: bill_spike – nếu Bills tăng bất thường so với ngưỡng 25% lương
        if (bills > 0.25 * salary) {
            r.add("bill_spike");
            r.category = "Bills";
            r.message = "Hóa đơn tháng này tăng cao, kiểm tra các khoản điện/nước/internet.";
        }

        log.info("rule_evaluated rulesHit={}", r.rulesHit);
        return r;
    }

    private double d(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0d; }
    }
}
