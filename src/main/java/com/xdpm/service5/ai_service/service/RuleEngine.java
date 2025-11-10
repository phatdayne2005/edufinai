// === Week 3: Bổ sung generateCoreMessage() & getHitRules() ===

package com.xdpm.service5.ai_service.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
public class RuleEngine {

    private List<String> lastHitRules = new ArrayList<>();

    // --------------------------------------------------------
    // 1️⃣ Evaluate bằng FeatureBundle (dành cho /dev/event)
    // --------------------------------------------------------
    public RuleResult evaluate(FeatureBuilder.FeatureBundle fb) {
        RuleResult result = evaluateInternal(fb.getSpendSalaryRatio().doubleValue(),
                fb.getLast30dTotal().doubleValue(),
                fb.getByCategory());
        lastHitRules = result.getRulesHit();
        return result;
    }

    // --------------------------------------------------------
    // 2️⃣ Evaluate từ Map (dành cho generateAndSave)
    // --------------------------------------------------------
    @SuppressWarnings("unchecked")
    public RuleResult evaluate(Map<String, Object> feats) {
        double ratio = toDouble(feats.get("spend_salary_ratio"));
        double salary = toDouble(feats.get("salary_month"));
        Map<String, Object> byCat = (Map<String, Object>) feats.getOrDefault("by_category", Map.of());
        RuleResult result = evaluateInternal(ratio, salary, byCat);
        lastHitRules = result.getRulesHit();
        return result;
    }

    // --------------------------------------------------------
    // 3️⃣ Core logic dùng chung
    // --------------------------------------------------------
    private RuleResult evaluateInternal(double ratio, double salary, Map<?, ?> byCatRaw) {
        Map<String, Object> byCat = (Map<String, Object>) byCatRaw;
        double food = toDouble(byCat.get("Food"));
        double bills = toDouble(byCat.get("Bills"));

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
        if (salary > 0 && food > 0.3 * salary) {
            rulesHit.add("food_overspend");
            category = "Food";
            suggestion = "Chi tiêu ăn uống vượt 30% lương, cân nhắc cắt giảm.";
            explanation = "Food > 30% salary.";
            score = 85;
        }
        if (salary > 0 && bills > 0.25 * salary) {
            rulesHit.add("bill_spike");
            category = "Bills";
            suggestion = "Hóa đơn tháng này tăng cao, kiểm tra điện/nước/internet.";
            explanation = "Bills vượt 25% lương.";
            score = 75;
        }

        RuleResult r = new RuleResult();
        r.setRuleIds(rulesHit);
        r.setRulesHit(rulesHit != null ? rulesHit : new ArrayList<>());        r.setCategory(category);
        r.setSuggestion(suggestion);
        r.setMessage(suggestion);
        r.setExplanation(explanation);
        r.setScore(score);

        log.info("[RuleEngine] rules_hit={} category={} score={}", rulesHit, category, score);
        return r;
    }

    // --------------------------------------------------------
    // 4️⃣ Hàm mới: generateCoreMessage() cho RULE_LLM mode
    // --------------------------------------------------------
    public String generateCoreMessage(Map<String, Object> feats) {
        RuleResult result = evaluate(feats);
        StringBuilder prompt = new StringBuilder();
        prompt.append("Phân tích chi tiêu người dùng: ");
        prompt.append(result.getExplanation()).append(". ");
        prompt.append("Gợi ý: ").append(result.getSuggestion()).append(". ");
        prompt.append("Các quy tắc áp dụng: ").append(String.join(", ", result.getRulesHit()));
        return prompt.toString();
    }

    // --------------------------------------------------------
    // 5️⃣ Getter cho hitRules (để service dùng lại)
    // --------------------------------------------------------
    public List<String> getHitRules() {
        return lastHitRules == null ? List.of() : lastHitRules;
    }

    // --------------------------------------------------------
    // 6️⃣ Helper
    // --------------------------------------------------------
    private double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return 0d;
        }
    }

    // --------------------------------------------------------
    // 7️⃣ Result DTO
    // --------------------------------------------------------
    @Data
    public static class RuleResult {
        private List<String> ruleIds;
        private List<String> rulesHit;
        private String suggestion;
        private String message;
        private String explanation;
        private String category;
        private int score;
    }
}
