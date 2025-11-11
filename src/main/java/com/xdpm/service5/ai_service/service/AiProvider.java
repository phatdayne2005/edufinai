package com.xdpm.service5.ai_service.service;

/**
 * Giao diện trừu tượng cho các nhà cung cấp AI NLG (Natural Language Generation).
 * Tuần 3: bổ sung khả năng log latency, fallback và mở rộng nhiều provider.
 */
public interface AiProvider {

    /**
     * Sinh văn bản gợi ý từ prompt đầu vào.
     *
     * @param prompt Nội dung đầu vào (ví dụ: rule-based message).
     * @return Câu gợi ý hoàn chỉnh hoặc văn bản fallback nếu lỗi.
     */
    String generateText(String prompt);

    /**
     * Trả về tên của provider đang được sử dụng (ví dụ: OpenAI, LocalLLM).
     * Giúp log và metrics xác định nguồn tạo nội dung.
     */
    default String getProviderName() {
        return this.getClass().getSimpleName();
    }
}
