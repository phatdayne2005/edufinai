package vn.uth.edufinai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * DTO cho một message trong conversation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    private Long id;
    private String conversationId;
    private String userId;
    private String question;
    private String answer;
    private List<String> tips;
    private List<String> disclaimers;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private ZonedDateTime createdAt;
}

