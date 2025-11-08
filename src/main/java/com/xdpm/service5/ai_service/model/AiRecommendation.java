package com.xdpm.service5.ai_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.GenericGenerator;

@Data
@Entity
@Table(name = "ai_recommendations")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendation {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "char(36)")
    private String id; // ← đổi lại từ aiId thành id

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "category", length = 50)
    private String category; // ví dụ: Saving / Food / Bills

    private String type;

    @Column(name = "message", columnDefinition = "TEXT") // ← đổi lại từ suggestion
    private String message;

    @Column(name = "analyzed_data", columnDefinition = "JSON")
    private String analyzedData;

    @Column(name = "chart_data", columnDefinition = "JSON")
    private String chartData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
