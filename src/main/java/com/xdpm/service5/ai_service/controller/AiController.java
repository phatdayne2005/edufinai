package com.xdpm.service5.ai_service.controller;

import com.xdpm.service5.ai_service.service.AiRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiRecommendationService service;

    public AiController(AiRecommendationService service) {
        this.service = service;
    }

    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> recommend(@RequestBody Map<String, Object> body) {
        String userId = (String) body.getOrDefault("user_id", "unknown");
        return ResponseEntity.ok(service.generateMockRecommendation(userId));
    }

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> report(@RequestParam String user_id) {
        return ResponseEntity.ok(service.getMockReport(user_id));
    }

    @GetMapping("/chart")
    public ResponseEntity<Map<String, Object>> chart(@RequestParam String user_id) {
        return ResponseEntity.ok(service.getMockChart(user_id));
    }
}
