package vn.uth.edufinai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import vn.uth.edufinai.dto.ReportResponse;
import vn.uth.edufinai.integration.GeminiClient;
import vn.uth.edufinai.model.ReportEntity;
import vn.uth.edufinai.repository.ReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    public ReportService(ReportRepository reportRepository, ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
    }

    public Mono<ReportResponse> getDailyReport(ZonedDateTime date) {
        return Mono.fromCallable(() -> {
                    // Normalize date về UTC và chỉ lấy date part
                    LocalDate targetDate = date != null 
                        ? date.toLocalDate() 
                        : java.time.LocalDate.now();
                    return reportRepository.findByReportDate(targetDate)
                            .map(this::toResponse)
                            .orElse(null);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<ReportResponse> saveSanitizedReport(ZonedDateTime date,
                                                    String prompt,
                                                    GeminiClient.Result aiResult,
                                                    OutputGuard.SanitizeResult sanitized,
                                                    Map<String, Object> metadata) {
        return Mono.fromCallable(() -> {
                    // Đảm bảo date được normalize về UTC và chỉ lấy date part (time = 00:00:00)
                    ZonedDateTime normalizedDate = date != null 
                        ? date.toLocalDate().atStartOfDay(java.time.ZoneId.of("UTC"))
                        : ZonedDateTime.now(java.time.ZoneId.of("UTC")).toLocalDate().atStartOfDay(java.time.ZoneId.of("UTC"));
                    
                    ReportEntity e = reportRepository.findByReportDate(normalizedDate.toLocalDate()).orElse(new ReportEntity());
                    e.setReportDate(normalizedDate); // Set với UTC và time = 00:00:00
                    e.setPrompt(prompt);
                    e.setRawSummary(aiResult.answerText != null ? aiResult.answerText : "");
                    e.setSanitizedSummary(sanitized.sanitizedText);
                    e.setModel(aiResult.model);
                    if (aiResult.usage != null) {
                        e.setUsagePromptTokens(aiResult.usage.promptTokens);
                        e.setUsageCompletionTokens(aiResult.usage.candidatesTokens);
                        e.setUsageTotalTokens(aiResult.usage.totalTokens);
                    }
                    e.setMetadataJson(metadata != null ? serializeMetadata(metadata) : null);
                    e.setCreatedAt(Optional.ofNullable(e.getCreatedAt()).orElse(ZonedDateTime.now(java.time.ZoneId.of("UTC"))));
                    e.setUpdatedAt(ZonedDateTime.now(java.time.ZoneId.of("UTC")));
                    ReportEntity saved = reportRepository.save(e);
                    return toResponse(saved);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private ReportResponse toResponse(ReportEntity e) {
        return ReportResponse.builder()
                .reportDate(e.getReportDate())
                .model(e.getModel())
                .rawSummary(e.getRawSummary())
                .sanitizedSummary(e.getSanitizedSummary())
                .usagePromptTokens(Optional.ofNullable(e.getUsagePromptTokens()).orElse(0))
                .usageCompletionTokens(Optional.ofNullable(e.getUsageCompletionTokens()).orElse(0))
                .usageTotalTokens(Optional.ofNullable(e.getUsageTotalTokens()).orElse(0))
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata: {}", e.getMessage());
            return metadata.toString();
        }
    }
}


