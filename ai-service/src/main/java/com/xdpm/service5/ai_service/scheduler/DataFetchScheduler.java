package com.xdpm.service5.ai_service.scheduler;

import com.xdpm.service5.ai_service.integration.GeminiClient;
import com.xdpm.service5.ai_service.processor.PromptBuilder;
import com.xdpm.service5.ai_service.service.OutputGuard;
import com.xdpm.service5.ai_service.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;

@Slf4j
@Component
public class DataFetchScheduler {

    private final WebClient.Builder webClientBuilder;
    private final PromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final OutputGuard outputGuard;
    private final ReportService reportService;

    @Value("${services.transaction.url:}")
    private String transactionServiceUrl;

    @Value("${services.userprofile.url:}")
    private String userProfileServiceUrl;

    @Value("${services.goals.url:}")
    private String goalsServiceUrl;

    @Value("${services.learning.url:}")
    private String learningServiceUrl;


    public DataFetchScheduler(WebClient.Builder webClientBuilder,
                              PromptBuilder promptBuilder,
                              GeminiClient geminiClient,
                              OutputGuard outputGuard,
                              ReportService reportService) {
        this.webClientBuilder = webClientBuilder;
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.outputGuard = outputGuard;
        this.reportService = reportService;
    }

    @Scheduled(cron = "${edufinai.scheduler.daily.cron:0 15 2 * * *}")
    public void runDaily() {
        // Sử dụng UTC để nhất quán
        ZonedDateTime today = ZonedDateTime.now(java.time.ZoneId.of("UTC"));
        log.info("[DAILY] Scheduler started for date: {}", today);

        PromptBuilder.DailySummaryInput input = new PromptBuilder.DailySummaryInput();
        input.reportDate = today;

        Mono<Map<String, Object>> txMono = fetchJson(transactionServiceUrl);
        Mono<Map<String, Object>> upMono = fetchJson(userProfileServiceUrl);
        Mono<Map<String, Object>> goalsMono = fetchJson(goalsServiceUrl);
        Mono<Map<String, Object>> learningMono = fetchJson(learningServiceUrl);

        Mono.zip(txMono.defaultIfEmpty(Map.of()),
                        upMono.defaultIfEmpty(Map.of()),
                        goalsMono.defaultIfEmpty(Map.of()),
                        learningMono.defaultIfEmpty(Map.of()))
                .flatMap(tuple -> {
                    input.transactions = tuple.getT1();
                    input.userProfile = tuple.getT2();
                    input.goals = tuple.getT3();
                    input.learning = tuple.getT4();

                    String prompt = promptBuilder.buildDailyReportPrompt(input);
                    return geminiClient.callGemini(prompt)
                            .flatMap(result -> {
                                OutputGuard.SanitizeResult sanitized = outputGuard.filterViolations(result.answerText);
                                Map<String, Object> metadata = Map.of(
                                        "violations", sanitized.violated,
                                        "flags", sanitized.flags
                                );
                                return reportService.saveSanitizedReport(today, prompt, result, sanitized, metadata)
                                        .doOnSuccess(r -> log.info("[DAILY] Saved report date={} model={} tokens={}",
                                                r.getReportDate(), r.getModel(), r.getUsageTotalTokens()))
                                        .doOnError(ex -> log.error("[DAILY] Save report failed: {}", ex.toString()));
                            });
                })
                .doOnError(ex -> log.error("[DAILY] Scheduler error: {}", ex.toString()))
                .onErrorResume(ex -> Mono.empty())
                .block();
    }

    private Mono<Map<String, Object>> fetchJson(String url) {
        if (url == null || url.isBlank()) {
            return Mono.empty();
        }
        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(8))
                .onErrorResume(ex -> {
                    log.debug("Downstream service unavailable: url={}", url);
                    return Mono.empty();
                });
    }
}



