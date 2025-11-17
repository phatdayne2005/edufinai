package vn.uth.edufinai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho toàn bộ lịch sử của một conversation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationHistory {
    private String conversationId;
    private String userId;
    private List<ChatMessage> messages;
}

