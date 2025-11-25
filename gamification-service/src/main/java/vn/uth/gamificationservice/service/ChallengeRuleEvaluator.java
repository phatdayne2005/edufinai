package vn.uth.gamificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import vn.uth.gamificationservice.dto.ChallengeEventRequest;
import vn.uth.gamificationservice.dto.ChallengeRule;

@Component
public class ChallengeRuleEvaluator {

    private final ObjectMapper objectMapper;

    public ChallengeRuleEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChallengeRule parse(String ruleJson) {
        try {
            return objectMapper.readValue(ruleJson, ChallengeRule.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid challenge rule JSON", e);
        }
    }

    public boolean matches(ChallengeRule rule, ChallengeEventRequest event) {
        if (rule == null || event == null) {
            logMatch(rule, event, "RULE_OR_EVENT_NULL", false);
            return false;
        }
        if (rule.getEventType() != null && !rule.getEventType().equalsIgnoreCase(event.getEventType())) {
            logMatch(rule, event, "EVENT_TYPE_MISMATCH", false);
            return false;
        }
        if (rule.getAction() != null && !rule.getAction().equalsIgnoreCase(event.getAction())) {
            logMatch(rule, event, "ACTION_MISMATCH", false);
            return false;
        }
        if (rule.getMinScore() != null) {
            if (event.getScore() == null || event.getScore() < rule.getMinScore()) {
                logMatch(rule, event, "MIN_SCORE_FAIL", false);
                return false;
            }
        }
        if (rule.getMaxScore() != null && event.getScore() != null && event.getScore() > rule.getMaxScore()) {
            logMatch(rule, event, "MAX_SCORE_FAIL", false);
            return false;
        }
        if (rule.getMinAccuracy() != null) {
            if (event.getAccuracyPercent() == null || event.getAccuracyPercent() < rule.getMinAccuracy()) {
                logMatch(rule, event, "MIN_ACCURACY_FAIL", false);
                return false;
            }
        }
        if (rule.getMaxAccuracy() != null && event.getAccuracyPercent() != null
                && event.getAccuracyPercent() > rule.getMaxAccuracy()) {
            logMatch(rule, event, "MAX_ACCURACY_FAIL", false);
            return false;
        }
        logMatch(rule, event, "MATCHED", true);
        return true;
    }

    private void logMatch(ChallengeRule rule, ChallengeEventRequest event, String reason, boolean matched) {
        org.slf4j.LoggerFactory.getLogger(ChallengeRuleEvaluator.class).debug(
                "ChallengeRule match result: matched={}, reason={}, rule={}, event={}",
                matched, reason, rule, sanitizeEvent(event));
    }

    private ChallengeEventRequest sanitizeEvent(ChallengeEventRequest event) {
        if (event == null) {
            return null;
        }
        ChallengeEventRequest safe = new ChallengeEventRequest();
        safe.setUserId(event.getUserId());
        safe.setEventType(event.getEventType());
        safe.setAction(event.getAction());
        safe.setLessonId(event.getLessonId());
        safe.setEnrollId(event.getEnrollId());
        safe.setScore(event.getScore());
        safe.setAccuracyPercent(event.getAccuracyPercent());
        safe.setTotalQuestions(event.getTotalQuestions());
        safe.setCorrectAnswers(event.getCorrectAnswers());
        safe.setAmount(event.getAmount());
        safe.setOccurredAt(event.getOccurredAt());
        return safe;
    }

    public int resolveTarget(ChallengeRule rule, Integer challengeTarget) {
        if (challengeTarget != null && challengeTarget > 0) {
            return challengeTarget;
        }
        // Fallback: nếu targetValue không có, dùng giá trị mặc định
        return 1;
    }
}

