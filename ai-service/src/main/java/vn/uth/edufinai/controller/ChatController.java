package vn.uth.edufinai.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import vn.uth.edufinai.dto.*;
import vn.uth.edufinai.integration.GeminiClient;
import vn.uth.edufinai.processor.PromptBuilder;
import vn.uth.edufinai.service.ChatHistoryService;
import vn.uth.edufinai.service.ChatResponseFormatter;
import vn.uth.edufinai.service.OutputGuard;

import java.util.Map;
import java.util.Optional;

import vn.uth.edufinai.util.Constants;
import vn.uth.edufinai.util.DateTimeUtils;
import vn.uth.edufinai.util.JwtUtils;
import vn.uth.edufinai.util.WebClientUtils;

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
    public Mono<ChatResponse> ask(@Valid @RequestBody ChatRequest req,
                                  Authentication authentication) {
        return JwtUtils.requireJwtMono(authentication)
                .flatMap(jwtAuth -> {
                    String userId = JwtUtils.extractUserId(jwtAuth);
                    String context = Optional.ofNullable(req.getContext())
                            .map(String::trim)
                            .filter(val -> !val.isEmpty())
                            .orElse(null);
                    String question = Optional.ofNullable(req.getQuestion())
                            .map(String::trim)
                            .orElse("");

                    if ((question == null || question.isEmpty()) && context == null) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Question cannot be blank unless context preset is provided"));
                    }

                    if (question == null || question.isEmpty()) {
                        question = defaultQuestionForContext(context);
                    }
                    final boolean skipHistory = isWidgetContext(context);
                    final String effectiveQuestion = question;
                    final String effectiveContext = context;
                    
                    // Lấy hoặc tạo conversation ID
                    String conversationId = skipHistory ? null :
                            Optional.ofNullable(req.getConversationId())
                                    .filter(id -> !id.trim().isEmpty())
                                    .orElseGet(() -> chatHistoryService.generateConversationId());
                    final String effectiveConversationId = conversationId;

                    // Lấy conversation history nếu có conversationId
                    Mono<String> historyMono = conversationId != null && !conversationId.isEmpty()
                            ? chatHistoryService.getConversationContext(conversationId, 10)
                                    .map(this::formatHistoryForPrompt)
                                    .defaultIfEmpty("")
                            : Mono.just("");

                    WebClient webClient = webClientBuilder.build();
                    return Mono.zip(
                            WebClientUtils.fetchUserScopedJson(webClient, transactionServiceUrl, jwtAuth),
                            WebClientUtils.fetchUserScopedJson(webClient, userProfileServiceUrl, jwtAuth),
                            WebClientUtils.fetchUserScopedJson(webClient, goalsServiceUrl, jwtAuth),
                            WebClientUtils.fetchUserScopedJson(webClient, learningServiceUrl, jwtAuth),
                            historyMono
                    ).flatMap(tuple -> {
                        PromptBuilder.ChatContext ctx = new PromptBuilder.ChatContext();
                        ctx.userId = userId;
                        ctx.question = effectiveQuestion;
                        ctx.presetContext = effectiveContext;
                        ctx.systemContext = Map.of("ts", DateTimeUtils.nowUtc().toString());
                        ctx.userData = Map.of(
                                "transactions", tuple.getT1(),
                                "userProfile", tuple.getT2(),
                                "goals", tuple.getT3(),
                                "learning", tuple.getT4()
                        );
                        ctx.conversationHistory = tuple.getT5();
                        
                        String prompt = promptBuilder.buildChatPrompt(ctx);
                        
                        return geminiClient.callGemini(prompt)
                                .flatMap(result -> {
                                    if (result == null || !result.ok) {
                                        String errorMsg = result != null ? result.errorMessage : "Gemini API returned null result";
                                        
                                        ChatResponse errorResponse = ChatResponse.builder()
                                                .userId(userId)
                                                .question(effectiveQuestion)
                                                .conversationId(effectiveConversationId)
                                                .answer(String.format("Xin lỗi, đã có lỗi xảy ra: %s", errorMsg))
                                                .tips(new java.util.ArrayList<>())
                                                .disclaimers(new java.util.ArrayList<>())
                                                .model(result != null ? result.model : null)
                                                .createdAt(DateTimeUtils.nowUtc())
                                                .build();
                                        return Mono.just(errorResponse);
                                    }
                                    
                                    String sanitizedText = outputGuard.filterViolations(result.answerText).sanitizedText;
                                    ChatResponse formattedResponse = responseFormatter.formatResponse(sanitizedText);
                                    formattedResponse.setUserId(userId);
                                    formattedResponse.setQuestion(effectiveQuestion);
                                    formattedResponse.setConversationId(effectiveConversationId);
                                    formattedResponse.setModel(result.model);
                                    formattedResponse.setPromptTokens(result.usage != null ? result.usage.promptTokens : null);
                                    formattedResponse.setCompletionTokens(result.usage != null ? result.usage.candidatesTokens : null);
                                    formattedResponse.setTotalTokens(result.usage != null ? result.usage.totalTokens : null);
                                    formattedResponse.setCreatedAt(DateTimeUtils.nowUtc());
                                    
                                    if (skipHistory) {
                                        return Mono.just(formattedResponse);
                                    }
                                    
                                    return chatHistoryService.saveMessage(
                                            effectiveConversationId,
                                            userId,
                                            effectiveQuestion,
                                            prompt,
                                            formattedResponse,
                                            result.answerText,
                                            sanitizedText
                                    ).doOnSuccess(saved -> 
                                            log.debug("Saved message to history: conversationId={}, messageId={}", 
                                                    effectiveConversationId, saved.getId())
                                    ).doOnError(error -> 
                                            log.warn("Failed to save message to history: {}", error.getMessage())
                                    ).thenReturn(formattedResponse);
                                });
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
    public Mono<java.util.List<ConversationSummary>> getConversations(Authentication authentication) {
        return JwtUtils.requireJwtMono(authentication)
                .flatMap(jwtAuth -> {
                    String userId = JwtUtils.extractUserId(jwtAuth);
        return chatHistoryService.getUserConversations(userId)
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
                });
    }

    /**
     * Lấy lịch sử của một conversation cụ thể
     */
    @GetMapping(value = "/conversations/{conversationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ConversationHistory> getConversationHistory(
            @PathVariable String conversationId,
            Authentication authentication) {
        return JwtUtils.requireJwtMono(authentication)
                .flatMap(jwtAuth -> {
                    String userId = JwtUtils.extractUserId(jwtAuth);
                    return chatHistoryService.getConversationHistory(conversationId, userId)
                            .switchIfEmpty(Mono.error(new org.springframework.web.server.ResponseStatusException(
                                    org.springframework.http.HttpStatus.NOT_FOUND,
                                    "Conversation not found: " + conversationId)));
                });
    }

    /**
     * Xóa một conversation (xóa tất cả messages trong conversation)
     */
    @DeleteMapping(value = "/conversations/{conversationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> deleteConversation(
            @PathVariable String conversationId,
            Authentication authentication) {
        return JwtUtils.requireJwtMono(authentication)
                .flatMap(jwtAuth -> {
                    String userId = JwtUtils.extractUserId(jwtAuth);
                    return chatHistoryService.deleteConversation(conversationId, userId)
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
                });
    }

    private String defaultQuestionForContext(String context) {
        if (context == null || context.isBlank()) {
            return "Hãy tư vấn tài chính dựa trên dữ liệu hiện có.";
        }
        return switch (context.trim().toUpperCase()) {
            case "SPENDING_WIDGET" ->
                    "Xin phân tích nhanh các khoản chi tiêu nổi bật và cảnh báo trong 7 ngày gần nhất.";
            case "SAVING_WIDGET" ->
                    "Tóm tắt tiến độ tiết kiệm hiện tại và gợi ý cách duy trì/đẩy nhanh mục tiêu.";
            case "GOAL_WIDGET" ->
                    "Cho biết mục tiêu tài chính nào cần ưu tiên nhất lúc này và vì sao.";
            default ->
                    "Hãy tư vấn tài chính dựa trên dữ liệu hiện có.";
        };
    }

    private boolean isWidgetContext(String context) {
        if (context == null) {
            return false;
        }
        return Constants.WIDGET_CONTEXTS.contains(context.trim().toUpperCase());
    }
}