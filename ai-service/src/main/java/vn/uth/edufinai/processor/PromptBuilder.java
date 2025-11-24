package vn.uth.edufinai.processor;

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
                Data Analyst EduFinAI. Phân tích dữ liệu ngày %s, tạo báo cáo ngắn gọn (3 dòng).
                
                Lưu ý về dữ liệu: cat=category, amt=amount, cnt=count, pct=percent
                
                Dữ liệu:
                - Giao dịch (7 ngày): %s
                - Học tập: %s
                
                Yêu cầu:
                - Trả về JSON thuần: {insight, rootCause, priorityAction}. Không dùng markdown.
                - insight: <=120 ký tự, điểm đáng chú ý nhất. Không lặp mẹo chung.
                - rootCause: giải thích dựa trên số liệu cụ thể.
                - priorityAction: hành động quan trọng nhất cần làm ngay.
                - Thiếu dữ liệu: insight="Chưa đủ dữ liệu", rootCause mô tả thiếu gì, priorityAction đề nghị đồng bộ.
                - Tiếng Việt, không trùng với widget khác.
                """, date, toJsonString(input.finance), toJsonString(input.learning));
    }

    public String buildChatPrompt(ChatContext ctx) {
        String historyContext = "";
        if (ctx.conversationHistory != null && !ctx.conversationHistory.trim().isEmpty()) {
            historyContext = String.format("""
                    
                    Lịch sử hội thoại:
                    %s
                    
                    """, ctx.conversationHistory);
        }

        String scenarioInstruction = buildPresetInstruction(ctx.presetContext);
        String questionBlock;
        if (scenarioInstruction != null) {
            questionBlock = String.format("""
                    Widget: %s
                    %s
                    Không có câu hỏi thì tự tạo nội dung từ dữ liệu.
                    Câu hỏi: %s
                    """, ctx.presetContext, scenarioInstruction, ctx.question != null ? ctx.question : "");
        } else {
            questionBlock = String.format("Câu hỏi: %s", ctx.question);
        }
        
        return String.format("""
                Cố vấn tài chính EduFinAI. Trả lời ngắn gọn, hành động cụ thể, dựa trên dữ liệu (nếu có).
                
                Lưu ý về dữ liệu: cat=category, amt=amount, cnt=count, pct=percent
                
                %s
                %s
                Ngữ cảnh: %s
                Dữ liệu: %s
                
                Trả về JSON thuần (không markdown):
                {answer: "Câu trả lời chính, thân thiện. Câu đầu tiên: chào hỏi và giới thiệu.",
                 tips: ["Mẹo cụ thể 1-3", "Gợi ý chủ đề tiếp theo"],
                 disclaimers: ["Tính tham khảo", "Khuyến khích tham khảo chuyên gia"]}
                
                Lưu ý:
                - JSON thuần, bắt đầu bằng {, kết thúc bằng }
                - answer: tự nhiên, thân thiện, trả lời trực tiếp. Có lịch sử thì tham khảo ngữ cảnh.
                - tips: 1-3 mẹo cụ thể, có thể hành động
                - disclaimers: 1-2 lưu ý tham khảo và trách nhiệm
                - Tiếng Việt
                """, questionBlock, historyContext, toJsonString(ctx.systemContext), toJsonString(ctx.userData));
    }

    private String buildPresetInstruction(String context) {
        if (context == null || context.isBlank()) {
            return null;
        }
        return switch (context.trim().toUpperCase()) {
            case "SPENDING_WIDGET" -> """
                    Thẻ "Phân tích chi tiêu" (~2 dòng).
                    - answer: <=2 câu, <=200 ký tự, hạng mục chi vượt chuẩn nhất 7 ngày + % tăng.
                    - tips: 1 gợi ý <=80 ký tự, hành động cụ thể.
                    - disclaimers: 1 câu ngắn.
                    - Không markdown, không danh sách dài. Thiếu dữ liệu: "Chưa đủ giao dịch để phân tích".
                    """;
            case "SAVING_WIDGET" -> """
                    Thẻ "Gợi ý tiết kiệm".
                    - answer: <=2 câu, tiến độ tiết kiệm (ngày, % mục tiêu) hoặc cảnh báo chậm.
                    - tips: 1 câu <=80 ký tự, hành động duy trì/tăng đóng góp.
                    - disclaimers: 1 câu ngắn.
                    - Không lặp mẹo chung như "tiết kiệm 10%% thu nhập".
                    """;
            case "GOAL_WIDGET" -> """
                    Thẻ "Mục tiêu tiếp theo".
                    - answer: <=2 câu, mục tiêu ưu tiên (gần deadline hoặc giảm % tiến độ) + số ngày còn lại.
                    - tips: 1 câu <=80 ký tự, hành động cho mục tiêu đó.
                    - disclaimers: 1 câu ngắn.
                    - Không markdown, không danh sách dài.
                    """;
            default -> null;
        };
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
        public Map<String, Object> finance;
        public Map<String, Object> learning;
    }

    /** Context cho Chat Advisor */
    public static class ChatContext {
        public String userId;
        public String question;
        public String presetContext;
        public Map<String, Object> systemContext;
        public Map<String, Object> userData;
        /** Lịch sử conversation trước đó (để AI có context) */
        public String conversationHistory;
    }
}
