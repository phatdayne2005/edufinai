package com.xdpm.service5.ai_service.repository;

import com.xdpm.service5.ai_service.model.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository truy cập bảng ai_recommendations.
 * Tuần 2–3: phục vụ lấy lịch sử khuyến nghị, report, chart, KPI.
 */
@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, String> {

    /**
     * Lấy tối đa 20 khuyến nghị gần nhất của user, sắp xếp theo thời gian giảm dần.
     */
    List<AiRecommendation> findTop20ByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Đếm số khuyến nghị trùng (cùng user, cùng message, cùng category).
     */
    long countByUserIdAndMessageAndCategory(String userId, String message, String category);

    // ------------------------------------------------------------------------
    // 🧩 Tuần 3 – KPI & Chart Aggregation
    // ------------------------------------------------------------------------

    /**
     * Lấy tất cả khuyến nghị của user trong khoảng thời gian (dùng cho /ai/report).
     */
    @Query("SELECT r FROM AiRecommendation r " +
            "WHERE r.userId = :userId AND r.createdAt BETWEEN :from AND :to " +
            "ORDER BY r.createdAt DESC")
    List<AiRecommendation> findByUserIdAndDateBetween(
            @Param("userId") String userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Lấy tất cả khuyến nghị 30 ngày gần nhất để build biểu đồ nếu cache chưa có.
     */
    @Query("SELECT r FROM AiRecommendation r " +
            "WHERE r.userId = :userId AND r.createdAt >= :from " +
            "ORDER BY r.createdAt DESC")
    List<AiRecommendation> findRecentForChart(
            @Param("userId") String userId,
            @Param("from") LocalDateTime from
    );
}
