package vn.uth.edufinai.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import vn.uth.edufinai.config.ServicesConfig;
import vn.uth.edufinai.dto.*;
import vn.uth.edufinai.integration.GeminiClient;
import vn.uth.edufinai.processor.PromptBuilder;
import vn.uth.edufinai.service.ChatHistoryService;
import vn.uth.edufinai.service.ChatResponseFormatter;
import vn.uth.edufinai.service.ContextAnalyzer;
import vn.uth.edufinai.service.OutputGuard;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import vn.uth.edufinai.util.DateTimeUtils;
import vn.uth.edufinai.util.JwtUtils;
import vn.uth.edufinai.util.WebClientUtils;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatController {

    private final WebClient.Builder webClientBuilder; // LoadBalanced - dùng cho service name (lb://SERVICE-NAME)
    private final WebClient.Builder plainWebClientBuilder; // Plain - dùng cho URL đầy đủ (http://...)
    private final ServicesConfig servicesConfig;
    private final PromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final OutputGuard outputGuard;
    private final ChatResponseFormatter responseFormatter;
    private final ChatHistoryService chatHistoryService;
    private final ContextAnalyzer contextAnalyzer;
    
    @Value("${edufinai.http.connectTimeoutMs:3000}")
    private int connectTimeoutMs;
    
    @Value("${edufinai.http.responseTimeoutMs:10000}")
    private int responseTimeoutMs;

    public ChatController(WebClient.Builder webClientBuilder,
                          @Qualifier("plainWebClientBuilder") WebClient.Builder plainWebClientBuilder,
                          ServicesConfig servicesConfig,
                          PromptBuilder promptBuilder,
                          GeminiClient geminiClient,
                          OutputGuard outputGuard,
                          ChatResponseFormatter responseFormatter,
                          ChatHistoryService chatHistoryService,
                          ContextAnalyzer contextAnalyzer) {
        this.webClientBuilder = webClientBuilder;
        this.plainWebClientBuilder = plainWebClientBuilder;
        this.servicesConfig = servicesConfig;
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.outputGuard = outputGuard;
        this.responseFormatter = responseFormatter;
        this.chatHistoryService = chatHistoryService;
        this.contextAnalyzer = contextAnalyzer;
        
        // Log để debug URL injection
        String financeUrl = servicesConfig.getFinance().getUrl();
        String learningUrl = servicesConfig.getLearning().getUrl();
        String gamificationUrl = servicesConfig.getGamification().getUrl();
        log.info("Service URLs configured - finance: {}, learning: {}, gamification: {}", 
                financeUrl != null && !financeUrl.isEmpty() ? financeUrl : "EMPTY",
                learningUrl != null && !learningUrl.isEmpty() ? learningUrl : "EMPTY",
                gamificationUrl != null && !gamificationUrl.isEmpty() ? gamificationUrl : "EMPTY");
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

                    // Validation: Nếu không có context VÀ không có question thì báo lỗi
                    if ((question == null || question.isEmpty()) && context == null) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Question cannot be blank unless context preset is provided"));
                    }

                    // Khi có context, không cần question - tự động tạo question mặc định từ context
                    if (question == null || question.isEmpty()) {
                        question = defaultQuestionForContext(context);
                        log.debug("Tự động tạo question từ context: context={}, question={}", context, question);
                    }
                    
                    // Khi có context, không lưu lịch sử (bất kỳ context nào)
                    final boolean skipHistory = context != null && !context.trim().isEmpty();
                    final String effectiveQuestion = question;
                    final String effectiveContext = context;
                    
                    // Lấy hoặc tạo conversation ID (chỉ khi không skip history)
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

                    // Phân tích context để quyết định service nào cần gọi
                    // Khi có context, chỉ gọi service tương ứng để tối ưu API calls và token usage
                    ContextAnalyzer.RequiredServices requiredServices = 
                            contextAnalyzer.analyzeRequiredServices(effectiveContext);
                    
                        log.info("Context-based service selection - context={}, question={}, requiredServices: finance={}, learning={}, gamification={}", 
                                effectiveContext, effectiveQuestion,
                                requiredServices.needFinance, requiredServices.needLearning, 
                                requiredServices.needGamification);

                        // Tự động chọn WebClient dựa trên URL:
                        // - Nếu URL bắt đầu bằng "lb://" → dùng LoadBalanced WebClient (resolve service name từ Eureka)
                        // - Ngược lại → dùng Plain WebClient (URL đầy đủ như http://localhost:9100)
                        String financeUrl = servicesConfig.getFinance().getUrl();
                        WebClient webClient = createWebClientForUrl(financeUrl);
                    
                    // Chỉ gọi các service cần thiết dựa trên context analysis
                    Mono<Map<String, Object>> financeMono = createServiceMono(
                            requiredServices.needFinance, webClient, financeUrl, jwtAuth);
                    Mono<Map<String, Object>> learningMono = createServiceMono(
                            requiredServices.needLearning, webClient, servicesConfig.getLearning().getUrl(), jwtAuth);
                    Mono<Map<String, Object>> gamificationMono = createServiceMono(
                            requiredServices.needGamification, webClient, servicesConfig.getGamification().getUrl(), jwtAuth);
                    
                    return Mono.zip(
                            financeMono,
                            learningMono,
                            gamificationMono,
                            historyMono
                    ).flatMap(tuple -> {
                        Map<String, Object> userData = buildUserData(requiredServices, tuple);
                        
                        // Log để debug
                        log.info("User data received - finance: {}, learning: {}, gamification: {}", 
                                tuple.getT1().isEmpty() ? "EMPTY" : "HAS_DATA",
                                tuple.getT2().isEmpty() ? "EMPTY" : "HAS_DATA",
                                tuple.getT3().isEmpty() ? "EMPTY" : "HAS_DATA");
                        log.debug("User data details: {}", userData);
                        
                        PromptBuilder.ChatContext ctx = buildChatContext(
                                userId, effectiveQuestion, effectiveContext, userData, tuple.getT4());
                        String prompt = promptBuilder.buildChatPrompt(ctx);
                        
                        return geminiClient.callGemini(prompt)
                                .flatMap(result -> processGeminiResponse(
                                        result, userId, effectiveQuestion, effectiveConversationId, 
                                        prompt, skipHistory));
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
                    return chatHistoryService.getUserConversations(userId);
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

    /**
     * Tạo question mặc định từ context
     * Khi có context, không cần truyền question - method này sẽ tự động tạo question phù hợp
     */
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
            default -> "Hãy tư vấn tài chính dựa trên dữ liệu hiện có.";
        };
    }

    /**
     * Tự động chọn WebClient dựa trên URL:
     * - Nếu URL bắt đầu bằng "lb://" → dùng LoadBalanced WebClient (resolve service name từ Eureka)
     * - Ngược lại → dùng Plain WebClient (URL đầy đủ như http://localhost:9100)
     * 
     * Lưu ý: Tạo WebClient hoàn toàn mới (không dùng builder từ config) để tránh LoadBalancer filter
     * tự động được thêm vào bởi Spring Cloud.
     */
    private WebClient createWebClientForUrl(String url) {
        if (url != null && url.startsWith("lb://")) {
            // Dùng LoadBalanced WebClient để resolve service name từ Eureka
            log.debug("Using LoadBalanced WebClient for service discovery: url={}", url);
            return webClientBuilder.build();
        } else {
            // Dùng Plain WebClient cho URL đầy đủ (mock server hoặc direct URL)
            // Tạo WebClient hoàn toàn mới để đảm bảo không có LoadBalancer filter
            log.debug("Using Plain WebClient for direct URL: url={}", url);
            
            // Tạo WebClient mới hoàn toàn, không dùng builder từ config
            HttpClient httpClient = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                    .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                    .doOnConnected(conn -> conn
                            .addHandlerLast(new ReadTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS)));
            
            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        }
    }

    /**
     * Tạo Mono cho service call - chỉ gọi nếu cần thiết
     */
    private Mono<Map<String, Object>> createServiceMono(
            boolean isRequired, WebClient webClient, String url, JwtAuthenticationToken jwtAuth) {
        return isRequired 
                ? WebClientUtils.fetchUserScopedJson(webClient, url, jwtAuth)
                : Mono.just(Map.of());
    }

    /**
     * Xây dựng userData map chỉ chứa dữ liệu liên quan
     */
    private Map<String, Object> buildUserData(
            ContextAnalyzer.RequiredServices requiredServices,
            reactor.util.function.Tuple4<Map<String, Object>, Map<String, Object>, 
                    Map<String, Object>, String> tuple) {
        Map<String, Object> userData = new java.util.HashMap<>();
        if (requiredServices.needFinance) {
            userData.put("finance", tuple.getT1());
        }
        if (requiredServices.needLearning) {
            userData.put("learning", tuple.getT2());
        }
        if (requiredServices.needGamification) {
            userData.put("gamification", tuple.getT3());
        }
        return userData;
    }

    /**
     * Xây dựng ChatContext cho prompt
     */
    private PromptBuilder.ChatContext buildChatContext(
            String userId, String question, String context, 
            Map<String, Object> userData, String conversationHistory) {
        PromptBuilder.ChatContext ctx = new PromptBuilder.ChatContext();
        ctx.userId = userId;
        ctx.question = question;
        ctx.presetContext = context;
        ctx.systemContext = Map.of("ts", DateTimeUtils.nowUtc().toString());
        ctx.userData = userData;
        ctx.conversationHistory = conversationHistory;
        return ctx;
    }

    /**
     * Xử lý response từ Gemini và lưu lịch sử nếu cần
     */
    private Mono<ChatResponse> processGeminiResponse(
            vn.uth.edufinai.integration.GeminiClient.Result result,
            String userId, String question, String conversationId, String prompt, boolean skipHistory) {
        if (result == null || !result.ok) {
            return Mono.just(createErrorResponse(result, userId, question, conversationId));
        }
        
        String sanitizedText = outputGuard.filterViolations(result.answerText).sanitizedText;
        ChatResponse formattedResponse = responseFormatter.formatResponse(sanitizedText);
        enrichResponse(formattedResponse, userId, question, conversationId, result);
        
        if (skipHistory) {
            return Mono.just(formattedResponse);
        }
        
        return chatHistoryService.saveMessage(
                conversationId, userId, question, prompt, formattedResponse, 
                result.answerText, sanitizedText)
                .doOnSuccess(saved -> log.debug("Saved message: conversationId={}, messageId={}", 
                        conversationId, saved.getId()))
                .doOnError(error -> log.warn("Failed to save message: {}", error.getMessage()))
                .thenReturn(formattedResponse);
    }

    /**
     * Tạo error response khi Gemini API lỗi
     */
    private ChatResponse createErrorResponse(
            vn.uth.edufinai.integration.GeminiClient.Result result,
            String userId, String question, String conversationId) {
        String errorMsg = result != null ? result.errorMessage : "Gemini API returned null result";
        return ChatResponse.builder()
                .userId(userId)
                .question(question)
                .conversationId(conversationId)
                .answer(String.format("Xin lỗi, đã có lỗi xảy ra: %s", errorMsg))
                .tips(new java.util.ArrayList<>())
                .disclaimers(new java.util.ArrayList<>())
                .model(result != null ? result.model : null)
                .createdAt(DateTimeUtils.nowUtc())
                .build();
    }

    /**
     * Bổ sung thông tin vào response
     */
    private void enrichResponse(ChatResponse response, String userId, String question, 
                               String conversationId, vn.uth.edufinai.integration.GeminiClient.Result result) {
        response.setUserId(userId);
        response.setQuestion(question);
        response.setConversationId(conversationId);
        response.setModel(result.model);
        if (result.usage != null) {
            response.setPromptTokens(result.usage.promptTokens);
            response.setCompletionTokens(result.usage.candidatesTokens);
            response.setTotalTokens(result.usage.totalTokens);
        }
        response.setCreatedAt(DateTimeUtils.nowUtc());
    }

}