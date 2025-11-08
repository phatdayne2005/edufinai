package com.xdpm.service5.ai_service.repository;

import com.xdpm.service5.ai_service.model.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, String> {
    List<AiRecommendation> findTop20ByUserIdOrderByCreatedAtDesc(String userId);
    long countByUserIdAndMessageAndCategory(String userId, String message, String category);
}