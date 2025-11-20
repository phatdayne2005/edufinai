package vn.uth.edufinai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.uth.edufinai.dto.ChatResponse;
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
            String cleanAnswer = cleanedJson
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();
            ChatResponse response = ChatResponse.builder()
                    .answer(cleanAnswer) // Fallback: dùng text đã clean làm answer
                    .tips(new ArrayList<>())
                    .disclaimers(new ArrayList<>())
                    .build();
            response.setAnswerJson(jsonString);
            return response;
        }
    }

    private static final String MARKDOWN_JSON_START = "```json";
    private static final String MARKDOWN_CODE_START = "```";
    private static final int MARKDOWN_JSON_START_LEN = MARKDOWN_JSON_START.length();
    private static final int MARKDOWN_CODE_START_LEN = MARKDOWN_CODE_START.length();

    /**
     * Extract JSON từ markdown code block nếu có
     * Hỗ trợ các format:
     * - ```json {...} ```
     * - ``` {...} ```
     * - {...} (plain JSON)
     */
    private String extractJsonFromMarkdown(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        String trimmed = text.trim();
        
        if (trimmed.startsWith(MARKDOWN_JSON_START)) {
            int end = trimmed.lastIndexOf(MARKDOWN_CODE_START);
            if (end > MARKDOWN_JSON_START_LEN) {
                return trimmed.substring(MARKDOWN_JSON_START_LEN, end).trim();
            }
        }
        
        if (trimmed.startsWith(MARKDOWN_CODE_START)) {
            int end = trimmed.lastIndexOf(MARKDOWN_CODE_START);
            if (end > MARKDOWN_CODE_START_LEN) {
                return trimmed.substring(MARKDOWN_CODE_START_LEN, end).trim();
            }
        }
        
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

