package vn.uth.edufinai.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_logs", indexes = {
        @Index(name = "idx_ai_logs_user_time", columnList = "user_id, created_at"),
        @Index(name = "idx_ai_logs_conversation", columnList = "conversation_id, created_at")
})
public class AiLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="conversation_id", length = 64)
    private String conversationId;

    @Column(name="user_id", length = 64)
    private String userId;

    @Lob
    @Column(name="question")
    private String question;

    @Lob
    @Column(name="prompt")
    private String prompt;

    @Column(name="model", length = 100)
    private String model;

    @Lob
    @Column(name="raw_answer")
    private String rawAnswer;

    @Lob
    @Column(name="sanitized_answer")
    private String sanitizedAnswer;

    /** Formatted answer (đã parse từ JSON) */
    @Lob
    @Column(name="formatted_answer")
    private String formattedAnswer;

    @Column(name="usage_prompt_tokens")
    private Integer usagePromptTokens;

    @Column(name="usage_completion_tokens")
    private Integer usageCompletionTokens;

    @Column(name="usage_total_tokens")
    private Integer usageTotalTokens;

    @Column(name="created_at")
    private ZonedDateTime createdAt;
}


