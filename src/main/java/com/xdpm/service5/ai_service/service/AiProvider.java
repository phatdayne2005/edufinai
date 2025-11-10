package com.xdpm.service5.ai_service.service;

public interface AiProvider {
    /**
     * Sinh văn bản gợi ý từ prompt đầu vào.
     * @param prompt Nội dung đầu vào (ví dụ: rule-based message)
     * @return Câu gợi ý hoàn chỉnh hoặc văn bản fallback
     */
    String generateText(String prompt);
}
