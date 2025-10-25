package com.xdpm.service5.ai_service.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ai_recommendations")
public class AiRecommendation {
    @Id
    private String aiId;

    private String userId;
    private String type;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(columnDefinition = "JSON")
    private String analyzedData;

    @Column(columnDefinition = "JSON")
    private String chartData;

    private LocalDateTime createdAt = LocalDateTime.now();
}
