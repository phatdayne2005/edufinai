package vn.uth.edufinai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import vn.uth.edufinai.util.JwtUtils;
import vn.uth.edufinai.util.WebClientUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import vn.uth.edufinai.dto.ReportResponse;
import vn.uth.edufinai.integration.GeminiClient;
import vn.uth.edufinai.processor.PromptBuilder;
import vn.uth.edufinai.service.OutputGuard;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final WebClient.Builder webClientBuilder;
    private final PromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final OutputGuard outputGuard;
    private final ObjectMapper objectMapper;

    @Value("${services.transaction.url:}")
    private String transactionServiceUrl;

    @Value("${services.userprofile.url:}")
    private String userProfileServiceUrl;

    @Value("${services.goals.url:}")
    private String goalsServiceUrl;

    @Value("${services.learning.url:}")
    private String learningServiceUrl;

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

                    WebClient webClient = webClientBuilder.build();
                    String txUrl = buildUri(transactionServiceUrl, reportDate, true).toString();
                    Mono<Map<String, Object>> txMono = WebClientUtils.fetchUserScopedJson(webClient, txUrl, jwtAuth);
                    Mono<Map<String, Object>> profileMono = WebClientUtils.fetchUserScopedJson(
                            webClient, userProfileServiceUrl, jwtAuth);
                    Mono<Map<String, Object>> goalsMono = WebClientUtils.fetchUserScopedJson(
                            webClient, goalsServiceUrl, jwtAuth);
                    Mono<Map<String, Object>> learningMono = WebClientUtils.fetchUserScopedJson(
                            webClient, learningServiceUrl, jwtAuth);

                    return Mono.zip(txMono, profileMono, goalsMono, learningMono)
                .flatMap(tuple -> {
                    PromptBuilder.DailySummaryInput input = new PromptBuilder.DailySummaryInput();
                    input.reportDate = targetDate;
                    input.transactions = tuple.getT1();
                    input.userProfile = tuple.getT2();
                    input.goals = tuple.getT3();
                    input.learning = tuple.getT4();

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

    private URI buildUri(String baseUrl, LocalDate dateParam, boolean appendDateParam) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        if (appendDateParam && dateParam != null) {
            builder.queryParam("date", dateParam);
        }
        return builder.build(true).toUri();
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

