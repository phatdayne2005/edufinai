package com.xdpm.service5.ai_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiNlgService implements AiProvider {

    @Override
    public String generateText(String prompt) {
        long start = System.currentTimeMillis();
        String response;
        try {
            // ⚙️ Giả lập xử lý LLM hoặc external API (OpenAI / local model)
            response = "[LLM Rephrase] " + prompt;
            Thread.sleep(200); // mô phỏng latency trung bình 200ms
            long latency = System.currentTimeMillis() - start;
            log.info("[AI_NLG] success latency={}ms prompt='{}'", latency, truncate(prompt));
            return response;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.error("[AI_NLG] failed latency={}ms fallback coreMsg reason={}", latency, e.getMessage());
            return prompt; // 🔁 fallback về rule output nếu lỗi
        }
    }

    /**
     * Truncate log text để tránh spam log khi prompt quá dài.
     */
    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 120 ? text.substring(0, 117) + "..." : text;
    }
}
