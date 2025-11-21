package vn.uth.edufinai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {
    private String userId;
    
    /** Conversation ID để tiếp tục cuộc hội thoại cũ. Nếu không có, sẽ tạo conversation mới */
    private String conversationId;

    private String question;

    /**
     * Ngữ cảnh/preset để backend quyết định prompt (ví dụ: SPENDING_WIDGET, SAVING_WIDGET, GOAL_WIDGET).
     * Nếu context được cung cấp thì question có thể để trống.
     */
    private String context;
}


