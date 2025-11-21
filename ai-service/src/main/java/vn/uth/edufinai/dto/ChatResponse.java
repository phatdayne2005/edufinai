package vn.uth.edufinai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    private String userId;
    private String question;
    
    /** Conversation ID của cuộc hội thoại này */
    private String conversationId;
    
    /** Câu trả lời chính từ AI (đã được format đẹp) */
    private String answer;
    
    /** Danh sách các mẹo/tips */
    private List<String> tips;
    
    /** Danh sách các lưu ý/disclaimers */
    private List<String> disclaimers;
    
    /** JSON string gốc trả về từ Gemini (đã sanitize) - giữ lại để backward compatible */
    @Deprecated
    private String answerJson;
    
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private ZonedDateTime createdAt;
    
    /**
     * Helper method để đảm bảo lists không null
     */
    public List<String> getTips() {
        if (tips == null) {
            tips = new ArrayList<>();
        }
        return tips;
    }
    
    public List<String> getDisclaimers() {
        if (disclaimers == null) {
            disclaimers = new ArrayList<>();
        }
        return disclaimers;
    }
}

