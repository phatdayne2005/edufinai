package com.xdpm.service5.ai_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xdpm.service5.ai_service.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service để format response từ Gemini API thành structured format đẹp hơn
 */
@Slf4j
@Service
public class ChatResponseFormatter {

    private final ObjectMapper objectMapper;

    public ChatResponseFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse JSON string từ Gemini và format thành structured ChatResponse
     * 
     * @param jsonString JSON string từ Gemini (format: {"answer": "...", "tips": [...], "disclaimers": [...]})
     *                   Có thể có markdown code block: ```json ... ``` hoặc ``` ... ```
     * @return ChatResponse với các field đã được parse và format
     */
    @SuppressWarnings("deprecation")
    public ChatResponse formatResponse(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            ChatResponse response = ChatResponse.builder()
                    .answer("")
                    .tips(new ArrayList<>())
                    .disclaimers(new ArrayList<>())
                    .build();
            response.setAnswerJson(jsonString);
            return response;
        }

        // Extract JSON từ markdown code block nếu có
        String cleanedJson = extractJsonFromMarkdown(jsonString);

        try {
            JsonNode root = objectMapper.readTree(cleanedJson);
            
            String answer = extractString(root, "answer", "");
            List<String> tips = extractStringList(root, "tips");
            List<String> disclaimers = extractStringList(root, "disclaimers");
            
            ChatResponse response = ChatResponse.builder()
                    .answer(answer)
                    .tips(tips)
                    .disclaimers(disclaimers)
                    .build();
            // Giữ lại JSON gốc để backward compatible
            response.setAnswerJson(jsonString);
            return response;
                    
        } catch (Exception e) {
            log.warn("Failed to parse JSON response from Gemini: {}", e.getMessage());
            // Nếu không parse được JSON, coi như toàn bộ text là answer
            // Loại bỏ markdown code block nếu có
            String cleanAnswer = cleanedJson.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            ChatResponse response = ChatResponse.builder()
                    .answer(cleanAnswer) // Fallback: dùng text đã clean làm answer
                    .tips(new ArrayList<>())
                    .disclaimers(new ArrayList<>())
                    .build();
            response.setAnswerJson(jsonString);
            return response;
        }
    }

    /**
     * Extract JSON từ markdown code block nếu có
     * Hỗ trợ các format:
     * - ```json {...} ```
     * - ``` {...} ```
     * - {...} (plain JSON)
     */
    private String extractJsonFromMarkdown(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String trimmed = text.trim();
        
        // Kiểm tra nếu có markdown code block với ```json
        if (trimmed.startsWith("```json")) {
            int start = trimmed.indexOf("```json") + 7; // Bỏ qua ```json
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        
        // Kiểm tra nếu có markdown code block với ```
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("```") + 3; // Bỏ qua ```
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        
        // Nếu không có markdown, trả về nguyên bản
        return trimmed;
    }

    /**
     * Extract string value từ JSON node
     */
    private String extractString(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            return defaultValue;
        }
        if (field.isTextual()) {
            return field.asText();
        }
        return field.toString();
    }

    /**
     * Extract list of strings từ JSON node
     */
    private List<String> extractStringList(JsonNode node, String fieldName) {
        List<String> result = new ArrayList<>();
        JsonNode field = node.path(fieldName);
        
        if (field.isMissingNode() || field.isNull()) {
            return result;
        }
        
        if (field.isArray()) {
            for (JsonNode item : field) {
                if (item.isTextual()) {
                    result.add(item.asText());
                } else if (!item.isNull()) {
                    result.add(item.toString());
                }
            }
        } else if (field.isTextual()) {
            // Nếu không phải array mà là string, thêm vào list
            result.add(field.asText());
        }
        
        return result;
    }
}

