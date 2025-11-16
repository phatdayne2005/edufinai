package com.xdpm.service5.ai_service.controller;

import com.xdpm.service5.ai_service.dto.ChatMessage;
import com.xdpm.service5.ai_service.dto.ChatRequest;
import com.xdpm.service5.ai_service.dto.ChatResponse;
import com.xdpm.service5.ai_service.dto.ConversationHistory;
import com.xdpm.service5.ai_service.dto.ConversationSummary;
import com.xdpm.service5.ai_service.integration.GeminiClient;
import com.xdpm.service5.ai_service.processor.PromptBuilder;
import com.xdpm.service5.ai_service.service.ChatHistoryService;
import com.xdpm.service5.ai_service.service.ChatResponseFormatter;
import com.xdpm.service5.ai_service.service.OutputGuard;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatController {

    private final WebClient.Builder webClientBuilder;
    private final PromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final OutputGuard outputGuard;
    private final ChatResponseFormatter responseFormatter;
    private final ChatHistoryService chatHistoryService;

    @Value("${services.transaction.url:}")
    private String transactionServiceUrl;

    @Value("${services.userprofile.url:}")
    private String userProfileServiceUrl;

    @Value("${services.goals.url:}")
    private String goalsServiceUrl;

    @Value("${services.learning.url:}")
    private String learningServiceUrl;

    public ChatController(WebClient.Builder webClientBuilder,
                          PromptBuilder promptBuilder,
                          GeminiClient geminiClient,
                          OutputGuard outputGuard,
                          ChatResponseFormatter responseFormatter,
                          ChatHistoryService chatHistoryService) {
        this.webClientBuilder = webClientBuilder;
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.outputGuard = outputGuard;
        this.responseFormatter = responseFormatter;
        this.chatHistoryService = chatHistoryService;
    }

    @PostMapping(value = "/ask",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ChatResponse> ask(@Valid @RequestBody ChatRequest req) {
        String userId = Optional.ofNullable(req.getUserId()).orElse("anonymous");
        String question = req.getQuestion();
        
        // Lấy hoặc tạo conversation ID
        String conversationId = Optional.ofNullable(req.getConversationId())
                .filter(id -> !id.trim().isEmpty())
                .orElseGet(() -> chatHistoryService.generateConversationId());

        // Lấy conversation history nếu có conversationId
        Mono<String> historyMono = conversationId != null && !conversationId.isEmpty()
                ? chatHistoryService.getConversationContext(conversationId, 10) // Lấy 10 messages gần nhất
                        .map(messages -> formatHistoryForPrompt(messages))
                        .defaultIfEmpty("")
                : Mono.just("");

        return Mono.zip(
                fetchOptional(transactionServiceUrl).defaultIfEmpty(Map.of()),
                fetchOptional(userProfileServiceUrl).defaultIfEmpty(Map.of()),
                fetchOptional(goalsServiceUrl).defaultIfEmpty(Map.of()),
                fetchOptional(learningServiceUrl).defaultIfEmpty(Map.of()),
                historyMono
        ).flatMap(tuple -> {
            PromptBuilder.ChatContext ctx = new PromptBuilder.ChatContext();
            ctx.userId = userId;
            ctx.question = question;
            ctx.systemContext = Map.of("ts", ZonedDateTime.now().toString());
            ctx.userData = Map.of(
                    "transactions", tuple.getT1(),
                    "userProfile", tuple.getT2(),
                    "goals", tuple.getT3(),
                    "learning", tuple.getT4()
            );
            ctx.conversationHistory = tuple.getT5(); // History context
            
            String prompt = promptBuilder.buildChatPrompt(ctx);
            
            return geminiClient.callGemini(prompt)
                    .flatMap(result -> {
                        if (result == null || !result.ok) {
                            String errorMsg = result != null ? result.errorMessage : "Gemini API returned null result";
                            
                            // Tạo error response với format đẹp
                            ChatResponse errorResponse = ChatResponse.builder()
                                    .userId(userId)
                                    .question(question)
                                    .conversationId(conversationId)
                                    .answer(String.format("Xin lỗi, đã có lỗi xảy ra: %s", errorMsg))
                                    .tips(new java.util.ArrayList<>())
                                    .disclaimers(new java.util.ArrayList<>())
                                    .model(result != null ? result.model : null)
                                    .createdAt(ZonedDateTime.now())
                                    .build();
                            return Mono.just(errorResponse);
                        }
                        
                        // Sanitize response từ Gemini
                        String sanitizedText = outputGuard.filterViolations(result.answerText).sanitizedText;
                        
                        // Format response thành structured format
                        ChatResponse formattedResponse = responseFormatter.formatResponse(sanitizedText);
                        formattedResponse.setUserId(userId);
                        formattedResponse.setQuestion(question);
                        formattedResponse.setConversationId(conversationId);
                        formattedResponse.setModel(result.model);
                        formattedResponse.setPromptTokens(result.usage != null ? result.usage.promptTokens : null);
                        formattedResponse.setCompletionTokens(result.usage != null ? result.usage.candidatesTokens : null);
                        formattedResponse.setTotalTokens(result.usage != null ? result.usage.totalTokens : null);
                        ZonedDateTime now = ZonedDateTime.now(java.time.ZoneId.of("UTC"));
                        formattedResponse.setCreatedAt(now);
                        
                        // Lưu vào history và đợi save xong trước khi trả về response
                        // Để đảm bảo conversation xuất hiện ngay trong danh sách
                        return chatHistoryService.saveMessage(
                                conversationId,
                                userId,
                                question,
                                prompt,
                                formattedResponse,
                                result.answerText,
                                sanitizedText
                        ).doOnSuccess(saved -> 
                                log.debug("Saved message to history: conversationId={}, messageId={}", conversationId, saved.getId())
                        ).doOnError(error -> 
                                log.warn("Failed to save message to history: {}", error.getMessage())
                        ).thenReturn(formattedResponse); // Trả về response sau khi save xong
                    });
        });
    }
    
    /**
     * Format conversation history thành text để đưa vào prompt
     */
    private String formatHistoryForPrompt(java.util.List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            sb.append("User: ").append(msg.getQuestion()).append("\n");
            sb.append("Assistant: ").append(msg.getAnswer()).append("\n");
            if (msg.getTips() != null && !msg.getTips().isEmpty()) {
                sb.append("Tips: ").append(String.join(", ", msg.getTips())).append("\n");
            }
            sb.append("---\n");
        }
        return sb.toString();
    }

    /**
     * Lấy danh sách conversations của user
     */
    @GetMapping(value = "/conversations", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<java.util.List<ConversationSummary>> getConversations(
            @RequestParam(required = false) String userId) {
        String targetUserId = Optional.ofNullable(userId).orElse("anonymous");
        return chatHistoryService.getUserConversations(targetUserId)
                .doOnNext(summaries -> {
                    // Log để debug timestamps
                    for (ConversationSummary summary : summaries) {
                        log.debug("API Response - conversationId={}, createdAt={}, updatedAt={}, createdAtEpoch={}, updatedAtEpoch={}", 
                                summary.getConversationId(),
                                summary.getCreatedAt(),
                                summary.getUpdatedAt(),
                                summary.getCreatedAt() != null ? summary.getCreatedAt().toEpochSecond() : "null",
                                summary.getUpdatedAt() != null ? summary.getUpdatedAt().toEpochSecond() : "null");
                    }
                });
    }

    /**
     * Lấy lịch sử của một conversation cụ thể
     */
    @GetMapping(value = "/conversations/{conversationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ConversationHistory> getConversationHistory(
            @PathVariable String conversationId) {
        return chatHistoryService.getConversationHistory(conversationId)
                .switchIfEmpty(Mono.error(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Conversation not found: " + conversationId)));
    }

    /**
     * Xóa một conversation (xóa tất cả messages trong conversation)
     */
    @DeleteMapping(value = "/conversations/{conversationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> deleteConversation(
            @PathVariable String conversationId) {
        return chatHistoryService.deleteConversation(conversationId)
                .flatMap(deleted -> {
                    if (deleted) {
                        return Mono.just(Map.of(
                                "success", true,
                                "message", "Conversation deleted successfully",
                                "conversationId", conversationId
                        ));
                    } else {
                        return Mono.error(new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND,
                                "Conversation not found: " + conversationId));
                    }
                });
    }

    private Mono<Map<String, Object>> fetchOptional(String url) {
        if (url == null || url.isBlank()) {
            return Mono.empty();
        }
        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> {
                    log.debug("Downstream service unavailable: url={}", url);
                    return Mono.empty();
                });
    }
}