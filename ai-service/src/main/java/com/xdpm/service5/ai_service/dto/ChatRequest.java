package com.xdpm.service5.ai_service.dto;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Question cannot be blank")
    private String question;
}


