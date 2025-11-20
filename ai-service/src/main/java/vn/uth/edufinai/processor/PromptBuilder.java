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
                Bạn là Data Analyst của ứng dụng EduFinAI.
                Hãy quan sát toàn bộ dữ liệu trong ngày %s và tạo ra thông điệp NGẮN GỌN phục vụ ô "Báo cáo hôm nay" (3 dòng).
                
                Dữ liệu cung cấp:
                - Tổng quan giao dịch: %s
                - Hồ sơ người dùng: %s
                - Mục tiêu tài chính: %s
                - Hoạt động học tập: %s
                                
                YÊU CẦU QUAN TRỌNG:
                - CHỈ trả về JSON thuần với đúng 3 trường bắt buộc: insight, rootCause, priorityAction.
                - Không sử dụng markdown/code block, bắt đầu bằng { và kết thúc bằng }.
                - insight: <=120 ký tự, mô tả điểm đáng chú ý nhất trong ngày (ví dụ: dòng tiền đảo chiều, mục tiêu tụt tiến độ). Tuyệt đối không lặp lại các mẹo chung chung ở phần "Tư vấn AI".
                - rootCause: giải thích vì sao insight xảy ra, phải bám số liệu cụ thể từ dữ liệu ở trên (ví dụ: giao dịch nào tăng, hạng mục nào không cập nhật).
                - priorityAction: hành động duy nhất quan trọng nhất mà người dùng nên làm ngay (ví dụ: “Xem lại mục tiêu A vì tiến độ giảm 10%%”).
                - Nếu thiếu dữ liệu: đặt insight = "Chưa đủ dữ liệu để tạo báo cáo hôm nay", rootCause mô tả dữ liệu thiếu, priorityAction đề nghị đồng bộ thêm giao dịch/mục tiêu.
                - Nội dung phải bằng tiếng Việt, không nhắc lại những thông tin đã hiển thị ở các widget khác.
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

        String scenarioInstruction = buildPresetInstruction(ctx.presetContext);
        String questionBlock;
        if (scenarioInstruction != null) {
            questionBlock = String.format("""
                    Chế độ thẻ tư vấn đặc biệt: %s
                    %s
                    Nếu không có câu hỏi đầu vào, hãy tự chủ động tạo nội dung dựa trên dữ liệu.
                    Mô tả câu hỏi (nếu có): %s
                    """, ctx.presetContext, scenarioInstruction, ctx.question != null ? ctx.question : "");
        } else {
            questionBlock = String.format("Câu hỏi hiện tại: %s", ctx.question);
        }
        
        return String.format("""
                Bạn là cố vấn tài chính thông minh của EduFinAI. Trả lời ngắn gọn, có hành động cụ thể, dựa trên dữ liệu người dùng (nếu có).
                %s
                %s
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
                - "answer": Phải tự nhiên, thân thiện, trả lời trực tiếp câu hỏi hoặc insight yêu cầu. Nếu có lịch sử, hãy tham khảo để trả lời phù hợp với ngữ cảnh.
                - "tips": 1-3 mẹo cụ thể, có thể hành động được
                - "disclaimers": 1-2 lưu ý về tính tham khảo và trách nhiệm
                - Tất cả nội dung phải bằng tiếng Việt
                """, questionBlock, historyContext, toJsonString(ctx.systemContext), toJsonString(ctx.userData));
    }

    private String buildPresetInstruction(String context) {
        if (context == null || context.isBlank()) {
            return null;
        }
        return switch (context.trim().toUpperCase()) {
            case "SPENDING_WIDGET" -> """
                    Mục tiêu: tạo thẻ "Phân tích chi tiêu" cho ô nhỏ (~2 dòng).
                    - answer: tối đa 2 câu, <= 200 ký tự, nêu hạng mục chi vượt chuẩn nhất trong 7 ngày và mức tăng %.
                    - tips: duy nhất 1 gợi ý, <= 80 ký tự, hành động cụ thể (ví dụ: "Giới hạn Giải trí 300k tuần tới").
                    - disclaimers: tối đa 1 câu ngắn.
                    - Không dùng markdown, không liệt kê danh sách dài, không ví dụ minh họa chung chung. Nếu thiếu dữ liệu, nêu rõ "Chưa đủ giao dịch để phân tích".
                    """;
            case "SAVING_WIDGET" -> """
                    Mục tiêu: thẻ "Gợi ý tiết kiệm" gọn gàng.
                    - answer: <= 2 câu, nhấn mạnh tiến độ tiết kiệm hiện tại (chuỗi ngày vay, % mục tiêu) hoặc cảnh báo chậm tiến độ.
                    - tips: 1 câu <= 80 ký tự, đưa hành động duy trì/đẩy nhanh đóng góp.
                    - disclaimers: 1 câu ngắn.
                    - Tuyệt đối không lặp lại mẹo chung chung như "tiết kiệm 10%% thu nhập".
                    """;
            case "GOAL_WIDGET" -> """
                    Mục tiêu: thẻ "Mục tiêu tiếp theo".
                    - answer: <= 2 câu, nói rõ mục tiêu nào cần ưu tiên (gần deadline hoặc giảm % tiến độ) và số ngày còn lại.
                    - tips: 1 câu <= 80 ký tự, đề xuất bước hành động cho mục tiêu đó.
                    - disclaimers: 1 câu ngắn.
                    - Không dùng định dạng markdown hay danh sách dài.
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
        public Map<String, Object> transactions;
        public Map<String, Object> userProfile;
        public Map<String, Object> goals;
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
