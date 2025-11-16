package com.xdpm.service5.ai_service.repository;

import com.xdpm.service5.ai_service.model.AiLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiLogRepository extends JpaRepository<AiLogEntity, Long> {
    
    /**
     * Lấy tất cả messages trong một conversation, sắp xếp theo thời gian
     */
    List<AiLogEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    
    /**
     * Projection interface để lấy conversationId và lastUpdated
     */
    interface ConversationIdWithLastUpdated {
        String getConversationId();
        java.sql.Timestamp getLastUpdated();
    }
    
    /**
     * Lấy danh sách conversation IDs của một user với thời gian cập nhật mới nhất,
     * sắp xếp theo thời gian cập nhật mới nhất
     * Sử dụng native query để đảm bảo GROUP BY và ORDER BY hoạt động đúng
     * Lưu ý: Sử dụng java.sql.Timestamp trong projection, sẽ convert sang ZonedDateTime trong service
     */
    @Query(value = "SELECT conversation_id as conversationId, MAX(created_at) as lastUpdated " +
           "FROM ai_logs " +
           "WHERE user_id = :userId AND conversation_id IS NOT NULL " +
           "GROUP BY conversation_id " +
           "ORDER BY MAX(created_at) DESC", nativeQuery = true)
    List<ConversationIdWithLastUpdated> findConversationIdsWithLastUpdatedByUserId(@Param("userId") String userId);
    
    /**
     * Lấy message đầu tiên của một conversation (để lấy title/preview)
     */
    Optional<AiLogEntity> findFirstByConversationIdOrderByCreatedAtAsc(String conversationId);
    
    /**
     * Lấy message mới nhất của một conversation
     */
    Optional<AiLogEntity> findFirstByConversationIdOrderByCreatedAtDesc(String conversationId);
    
    /**
     * Đếm số messages trong một conversation
     */
    long countByConversationId(String conversationId);
    
    /**
     * Xóa tất cả messages trong một conversation
     */
    void deleteByConversationId(String conversationId);
}

