package com.xdpm.service5.ai_service.repository;

import com.xdpm.service5.ai_service.model.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy cập bảng ai_recommendations.
 * Tuần 2: phục vụ lấy lịch sử khuyến nghị, report, chart.
 */
@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, String> {

    /**
     * Lấy tối đa 20 khuyến nghị gần nhất của user, sắp xếp theo thời gian giảm dần.
     */
    List<AiRecommendation> findTop20ByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Đếm số khuyến nghị trùng (cùng user, cùng message, cùng category).
     * Có thể dùng để kiểm tra trùng nội dung nếu cần.
     */
    long countByUserIdAndMessageAndCategory(String userId, String message, String category);
}
