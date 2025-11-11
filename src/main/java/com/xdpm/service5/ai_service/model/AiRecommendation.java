package com.xdpm.service5.ai_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ai_recommendations")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String ruleHits;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    private int score;

    @Column(length = 50)
    private String category; // Saving / Food / Bills

    @Column(length = 50)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "analyzed_data", columnDefinition = "JSON")
    private String analyzedData;

    @Column(name = "chart_data", columnDefinition = "JSON")
    private String chartData;

    // 🧩 Thêm mới cho tuần 3 — flag kết quả kiểm tra an toàn nội dung (OutputGuard)
    @Column(name = "guard_pass")
    private Boolean guardPass;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
