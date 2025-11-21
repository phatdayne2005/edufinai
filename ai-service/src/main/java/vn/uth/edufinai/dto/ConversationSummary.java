package vn.uth.edufinai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * DTO cho summary của một conversation (dùng trong danh sách conversations)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSummary {
    private String conversationId;
    private String userId;
    private String title; // Câu hỏi đầu tiên hoặc preview
    private Long messageCount;
    private ZonedDateTime createdAt; // Thời gian tạo conversation
    private ZonedDateTime updatedAt; // Thời gian message cuối cùng
    private String relativeTime; // Thời gian tương đối (ví dụ: "2 phút trước", "Hôm qua")
}

