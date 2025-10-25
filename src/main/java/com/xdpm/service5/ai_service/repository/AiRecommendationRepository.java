package com.xdpm.service5.ai_service.repository;

import com.xdpm.service5.ai_service.model.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, String> {
}