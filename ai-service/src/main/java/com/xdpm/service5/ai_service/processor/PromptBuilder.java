package com.xdpm.service5.ai_service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Xây dựng prompt theo mẫu cho:
 * - Daily Report (Data Analyst pipeline)
 * - Chat Advisor (real-time)
 */
@Slf4j
@Component
public class PromptBuilder {

    private final ObjectMapper objectMapper;

    public PromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildDailyReportPrompt(DailySummaryInput input) {
        ZonedDateTime date = input.reportDate != null ? input.reportDate : ZonedDateTime.now();
        return String.format("""
                Bạn là Data Analyst của ứng dụng EduFinAI.
                Hãy tạo báo cáo tóm tắt tài chính & học tập cho ngày %s.
                - Tổng quan giao dịch: %s
                - Hồ sơ người dùng: %s
                - Mục tiêu tài chính: %s
                - Hoạt động học tập: %s
                Đầu ra yêu cầu JSON có dạng:
                {"highlights": [...], "risks": [...], "kpis": {...}, "advice": [...]}
                """, date, toJsonString(input.transactions), toJsonString(input.userProfile),
                toJsonString(input.goals), toJsonString(input.learning));
    }

    public String buildChatPrompt(ChatContext ctx) {
        String historyContext = "";
        if (ctx.conversationHistory != null && !ctx.conversationHistory.trim().isEmpty()) {
            historyContext = String.format("""
                    
                    Lịch sử cuộc hội thoại trước đó (để bạn có context):
                    %s
                    
                    """, ctx.conversationHistory);
        }
        
        return String.format("""
                Bạn là cố vấn tài chính thông minh của EduFinAI. Trả lời ngắn gọn, có hành động cụ thể, dựa trên dữ liệu người dùng (nếu có).
                %s
                Câu hỏi hiện tại: %s
                Ngữ cảnh hệ thống: %s
                Dữ liệu người dùng liên quan: %s
                
                Yêu cầu trả về JSON với format sau (CHỈ trả về JSON thuần, KHÔNG dùng markdown code block):
                {
                  "answer": "Câu trả lời chính, thân thiện và hữu ích. Nếu là câu hỏi đầu tiên, hãy chào hỏi và giới thiệu bản thân.",
                  "tips": [
                    "Mẹo hoặc gợi ý cụ thể, hữu ích cho người dùng (1-3 mẹo)",
                    "Có thể gợi ý các chủ đề họ có thể hỏi tiếp"
                  ],
                  "disclaimers": [
                    "Lưu ý rõ ràng về tính tham khảo và giáo dục của thông tin",
                    "Khuyến khích tham khảo chuyên gia tài chính trước khi quyết định quan trọng"
                  ]
                }
                
                Lưu ý QUAN TRỌNG:
                - CHỈ trả về JSON thuần, KHÔNG dùng markdown code block (```json hoặc ```)
                - Bắt đầu trực tiếp bằng { và kết thúc bằng }
                - "answer": Phải tự nhiên, thân thiện, trả lời trực tiếp câu hỏi. Nếu có lịch sử, hãy tham khảo để trả lời phù hợp với ngữ cảnh.
                - "tips": 1-3 mẹo cụ thể, có thể hành động được
                - "disclaimers": 1-2 lưu ý về tính tham khảo và trách nhiệm
                - Tất cả nội dung phải bằng tiếng Việt
                """, historyContext, ctx.question, toJsonString(ctx.systemContext), toJsonString(ctx.userData));
    }

    private String toJsonString(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return obj.toString();
        }
    }

    /** Input cho Daily Report */
    public static class DailySummaryInput {
        public ZonedDateTime reportDate;
        public Map<String, Object> transactions;
        public Map<String, Object> userProfile;
        public Map<String, Object> goals;
        public Map<String, Object> learning;
    }

    /** Context cho Chat Advisor */
    public static class ChatContext {
        public String userId;
        public String question;
        public Map<String, Object> systemContext;
        public Map<String, Object> userData;
        /** Lịch sử conversation trước đó (để AI có context) */
        public String conversationHistory;
    }
}
