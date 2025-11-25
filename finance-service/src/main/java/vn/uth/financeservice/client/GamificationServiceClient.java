package vn.uth.financeservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class GamificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(GamificationServiceClient.class);

    private final RestTemplate restTemplate;
    private final String gamificationServiceUrl;

    public GamificationServiceClient(RestTemplate restTemplate,
                                     @Value("${services.gamification.base-url:http://GAMIFICATION-SERVICE}") String gamificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.gamificationServiceUrl = gamificationServiceUrl;
    }

    /**
     * Publish challenge event khi user đạt mục tiêu tài chính
     * 
     * @param userId ID của user
     * @param eventType Loại event (GOAL, EXPENSE)
     * @param action Hành động (COMPLETE, ACHIEVE)
     */
    public void publishGoalAchievedEvent(UUID userId, String eventType, String action) {
        try {
            String url = gamificationServiceUrl + "/api/v1/gamify/challenge/event";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId.toString());
            requestBody.put("eventType", eventType);
            requestBody.put("action", action);
            requestBody.put("amount", 1); // Ghi nhận 1 lần đạt mục tiêu
            requestBody.put("occurredAt", ZonedDateTime.now().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            restTemplate.postForEntity(url, request, Map.class);
            log.debug("Published goal achieved event for user {}: eventType={}, action={}", userId, eventType, action);
        } catch (Exception e) {
            // Log error nhưng không throw để không ảnh hưởng đến flow chính
            log.warn("Failed to publish goal achieved event to gamification service for user {}: {}", userId, e.getMessage());
        }
    }
}

