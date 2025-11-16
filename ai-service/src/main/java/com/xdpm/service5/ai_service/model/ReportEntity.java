package com.xdpm.service5.ai_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.xdpm.service5.ai_service.config.ZonedDateTimeAttributeConverter;
import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_reports",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_reports_date", columnNames = "report_date"))
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="report_date", nullable = false)
    @Convert(converter = ZonedDateTimeAttributeConverter.class)
    private ZonedDateTime reportDate;

    @Column(name="model", length = 100)
    private String model;

    @Lob
    @Column(name="prompt")
    private String prompt;

    @Lob
    @Column(name="raw_summary")
    private String rawSummary;

    @Lob
    @Column(name="sanitized_summary", nullable = false)
    private String sanitizedSummary;

    @Column(name="usage_prompt_tokens")
    private Integer usagePromptTokens;

    @Column(name="usage_completion_tokens")
    private Integer usageCompletionTokens;

    @Column(name="usage_total_tokens")
    private Integer usageTotalTokens;

    @Column(name="metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name="created_at")
    private ZonedDateTime createdAt;

    @Column(name="updated_at")
    private ZonedDateTime updatedAt;
}
