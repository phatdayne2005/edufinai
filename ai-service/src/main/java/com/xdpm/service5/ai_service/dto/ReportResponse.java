package com.xdpm.service5.ai_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private ZonedDateTime reportDate;
    private String model;
    private String rawSummary;
    private String sanitizedSummary;

    private Integer usagePromptTokens;
    private Integer usageCompletionTokens;
    private Integer usageTotalTokens;

    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}

