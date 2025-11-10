package com.xdpm.service5.ai_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiNlgService implements AiProvider {

    @Override
    public String generateText(String prompt) {
        long start = System.currentTimeMillis();
        try {
            // ⚙️ Giả lập xử lý LLM hoặc external API (OpenAI / local model)
            String response = "[LLM Rephrase] " + prompt;
            Thread.sleep(200); // mô phỏng latency trung bình
            log.info("ai_nlg_latency_ms={}", System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            log.warn("LLM timeout, fallback to core message");
            return prompt; // fallback về rule output nếu lỗi
        }
    }
}
