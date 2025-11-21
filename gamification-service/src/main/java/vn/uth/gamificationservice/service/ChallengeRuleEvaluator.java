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
            return false;
        }
        if (rule.getEventType() != null && !rule.getEventType().equalsIgnoreCase(event.getEventType())) {
            return false;
        }
        if (rule.getAction() != null && !rule.getAction().equalsIgnoreCase(event.getAction())) {
            return false;
        }
        if (rule.getMinScore() != null) {
            if (event.getScore() == null || event.getScore() < rule.getMinScore()) {
                return false;
            }
        }
        if (rule.getMaxScore() != null && event.getScore() != null && event.getScore() > rule.getMaxScore()) {
            return false;
        }
        return true;
    }

    public int resolveTarget(ChallengeRule rule, Integer challengeTarget) {
        if (challengeTarget != null && challengeTarget > 0) {
            return challengeTarget;
        }
        if (rule != null && rule.getCount() != null && rule.getCount() > 0) {
            return rule.getCount();
        }
        return 1;
    }
}

