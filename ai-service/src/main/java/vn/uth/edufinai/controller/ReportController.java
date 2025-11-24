package vn.uth.edufinai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import vn.uth.edufinai.config.ServicesConfig;

import vn.uth.edufinai.util.JwtUtils;
import vn.uth.edufinai.util.WebClientUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import vn.uth.edufinai.dto.ReportResponse;
import vn.uth.edufinai.integration.GeminiClient;
import vn.uth.edufinai.processor.PromptBuilder;
import vn.uth.edufinai.service.OutputGuard;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final WebClient.Builder webClientBuilder; // LoadBalanced - dùng cho service name (lb://SERVICE-NAME)
    @Qualifier("plainWebClientBuilder")
    private final WebClient.Builder plainWebClientBuilder; // Plain - dùng cho URL đầy đủ (http://...)
    private final ServicesConfig servicesConfig;
    private final PromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final OutputGuard outputGuard;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/daily", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ReportResponse> getDailyReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        return JwtUtils.requireJwtMono(authentication)
                .flatMap(jwtAuth -> {
                    LocalDate reportDate = date != null ? date : LocalDate.now();
                    ZonedDateTime targetDate = reportDate.atStartOfDay(ZoneId.systemDefault());
                    String userId = JwtUtils.extractUserId(jwtAuth);
                    log.debug("[REPORT_ON_DEMAND] userId={} date={}", userId, reportDate);

                    // Tự động chọn WebClient dựa trên URL:
                    // - Nếu URL bắt đầu bằng "lb://" → dùng LoadBalanced WebClient (resolve service name từ Eureka)
                    // - Ngược lại → dùng Plain WebClient (URL đầy đủ như http://localhost:9100)
                    String financeUrl = servicesConfig.getFinance().getUrl();
                    WebClient webClient = createWebClientForUrl(financeUrl);
                    // Finance Service: GET /api/summary/7days (không cần date param - tự động lấy 7 ngày gần nhất)
                    Mono<Map<String, Object>> financeMono = WebClientUtils.fetchUserScopedJson(
                            webClient, financeUrl, jwtAuth);
                    Mono<Map<String, Object>> learningMono = WebClientUtils.fetchUserScopedJson(
                            webClient, servicesConfig.getLearning().getUrl(), jwtAuth);

                    return Mono.zip(financeMono, learningMono)
                .flatMap(tuple -> {
                    PromptBuilder.DailySummaryInput input = new PromptBuilder.DailySummaryInput();
                    input.reportDate = targetDate;
                    input.finance = tuple.getT1();
                    input.learning = tuple.getT2();

                    String prompt = promptBuilder.buildDailyReportPrompt(input);
                    return geminiClient.callGemini(prompt)
                            .flatMap(result -> {
                                if (!result.ok) {
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.SERVICE_UNAVAILABLE,
                                            String.format("Gemini call failed: %s", result.errorMessage)));
                                }

                                OutputGuard.SanitizeResult sanitized = outputGuard.filterViolations(result.answerText);
                                Map<String, String> summaryFields = extractSummaryFields(sanitized.sanitizedText);
                                ReportResponse response = ReportResponse.builder()
                                        .reportDate(targetDate)
                                        .model(result.model)
                                        .rawSummary(result.answerText)
                                        .sanitizedSummary(sanitized.sanitizedText)
                                        .insight(summaryFields.get("insight"))
                                        .rootCause(summaryFields.get("rootCause"))
                                        .priorityAction(summaryFields.get("priorityAction"))
                                        .usagePromptTokens(result.usage != null ? result.usage.promptTokens : null)
                                        .usageCompletionTokens(result.usage != null ? result.usage.candidatesTokens : null)
                                        .usageTotalTokens(result.usage != null ? result.usage.totalTokens : null)
                                        .createdAt(result.timestamp)
                                        .updatedAt(result.timestamp)
                                        .build();
                                return Mono.just(response);
                            });
                });
                });
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
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .responseTimeout(Duration.ofMillis(10000))
                    .doOnConnected(conn -> conn
                            .addHandlerLast(new ReadTimeoutHandler(10000, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(10000, TimeUnit.MILLISECONDS)));
            
            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        }
    }

    private Map<String, String> extractSummaryFields(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode node = objectMapper.readTree(jsonString);
            if (!node.isObject()) {
                return Map.of();
            }
            Map<String, String> result = new HashMap<>();
            node.fieldNames().forEachRemaining(field -> {
                JsonNode value = node.get(field);
                if (value != null && value.isTextual()) {
                    result.put(field, value.asText());
                }
            });
            return result;
        } catch (Exception ex) {
            log.warn("[REPORT_ON_DEMAND] Failed to parse summary JSON: {}", ex.getMessage());
            return Map.of();
        }
    }
}

