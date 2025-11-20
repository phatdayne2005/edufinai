package vn.uth.edufinai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import vn.uth.edufinai.dto.ChatMessage;
import vn.uth.edufinai.dto.ChatResponse;
import vn.uth.edufinai.dto.ConversationHistory;
import vn.uth.edufinai.dto.ConversationSummary;
import vn.uth.edufinai.model.AiLogEntity;
import vn.uth.edufinai.repository.AiLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.uth.edufinai.util.Constants;
import vn.uth.edufinai.util.DateTimeUtils;

import static vn.uth.edufinai.util.DateTimeUtils.UTC;

/**
 * Service để quản lý lịch sử chat và conversations
 */
@Slf4j
@Service
public class ChatHistoryService {

    private final AiLogRepository aiLogRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public ChatHistoryService(AiLogRepository aiLogRepository, 
                             ObjectMapper objectMapper,
                             TransactionTemplate transactionTemplate) {
        this.aiLogRepository = aiLogRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Tạo conversation ID mới
     */
    public String generateConversationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Lưu message vào history
     */
    public Mono<AiLogEntity> saveMessage(String conversationId, String userId, String question, 
                                        String prompt, ChatResponse chatResponse, String rawAnswer, 
                                        String sanitizedAnswer) {
        return Mono.fromCallable(() -> {
            AiLogEntity entity = new AiLogEntity();
            entity.setConversationId(conversationId);
            entity.setUserId(userId);
            entity.setQuestion(question);
            entity.setPrompt(prompt);
            entity.setModel(chatResponse.getModel());
            entity.setRawAnswer(rawAnswer);
            entity.setSanitizedAnswer(sanitizedAnswer);
            entity.setFormattedAnswer(chatResponse.getAnswer());
            entity.setUsagePromptTokens(chatResponse.getPromptTokens());
            entity.setUsageCompletionTokens(chatResponse.getCompletionTokens());
            entity.setUsageTotalTokens(chatResponse.getTotalTokens());
            entity.setCreatedAt(DateTimeUtils.nowUtc());
            
            return aiLogRepository.save(entity);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Lấy lịch sử conversation
     */
    public Mono<ConversationHistory> getConversationHistory(String conversationId, String userId) {
        return Mono.fromCallable(() -> {
            List<AiLogEntity> entities = aiLogRepository.findByConversationIdAndUserIdOrderByCreatedAtAsc(conversationId, userId);

            if (entities.isEmpty()) {
                return null;
            }
            
            List<ChatMessage> messages = new ArrayList<>();
            for (AiLogEntity entity : entities) {
                ChatMessage message = toChatMessage(entity);
                messages.add(message);
            }
            
            return ConversationHistory.builder()
                    .conversationId(conversationId)
                    .userId(userId)
                    .messages(messages)
                    .build();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Lấy danh sách conversations của user
     */
    public Mono<List<ConversationSummary>> getUserConversations(String userId) {
        return Mono.fromCallable(() -> {
            // Sử dụng query mới với GROUP BY để lấy conversationId và lastUpdated
            List<AiLogRepository.ConversationIdWithLastUpdated> conversations =
                aiLogRepository.findConversationIdsWithLastUpdatedByUserId(userId);
            
            List<ConversationSummary> summaries = new ArrayList<>();
            
            for (var conv : conversations) {
                String conversationId = conv.getConversationId();
                
                ZonedDateTime lastUpdated = null;
                if (conv.getLastUpdated() != null) {
                    try {
                        lastUpdated = conv.getLastUpdated().toInstant().atZone(UTC);
                        // Validate: đảm bảo timestamp không phải epoch (1970-01-01)
                        if (lastUpdated.toEpochSecond() <= 0) {
                            log.warn("Invalid lastUpdated timestamp (epoch) for conversationId={}, raw={}", 
                                    conversationId, conv.getLastUpdated());
                            lastUpdated = null; // Set null để fallback
                        }
                    } catch (Exception e) {
                        log.error("Error converting lastUpdated timestamp for conversationId={}, raw={}, error={}", 
                                conversationId, conv.getLastUpdated(), e.getMessage());
                        lastUpdated = null;
                    }
                }
                
                Optional<AiLogEntity> firstMessage = aiLogRepository.findFirstByConversationIdAndUserIdOrderByCreatedAtAsc(conversationId, userId);
                long messageCount = aiLogRepository.countByConversationIdAndUserId(conversationId, userId);
                
                if (firstMessage.isPresent()) {
                    String title = firstMessage.get().getQuestion();
                    if (title != null && title.length() > Constants.MAX_CONVERSATION_TITLE_LENGTH) {
                        title = title.substring(0, Constants.MAX_CONVERSATION_TITLE_LENGTH) + "...";
                    }
                    
                    ZonedDateTime createdAt = normalizeZonedDateTime(
                            firstMessage.get().getCreatedAt(), 
                            conversationId, 
                            "createdAt"
                    );
                    
                    // Nếu lastUpdated null hoặc invalid, dùng createdAt
                    if (lastUpdated == null) {
                        lastUpdated = createdAt;
                    }
                    
                    String relativeTimeStr = formatRelativeTime(lastUpdated != null ? lastUpdated : createdAt);
                    
                    log.debug("Conversation summary: conversationId={}, createdAt={}, updatedAt={}, relativeTime={}", 
                            conversationId, createdAt, lastUpdated, relativeTimeStr);
                    
                    summaries.add(ConversationSummary.builder()
                            .conversationId(conversationId)
                            .userId(userId)
                            .title(title != null ? title : "New Conversation")
                            .messageCount(messageCount)
                            .createdAt(createdAt) // Đảm bảo không null
                            .updatedAt(lastUpdated) // Đảm bảo không null (fallback to createdAt)
                            .relativeTime(relativeTimeStr)
                            .build());
                }
            }
            
            return summaries;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Lấy context từ conversation history (để đưa vào prompt)
     * Note: Method này không filter theo userId vì dùng cho context trong prompt
     */
    public Mono<List<ChatMessage>> getConversationContext(String conversationId, int limit) {
        return Mono.fromCallable(() -> {
            List<AiLogEntity> entities = aiLogRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
            
            int start = Math.max(0, entities.size() - limit);
            List<AiLogEntity> recentEntities = entities.subList(start, entities.size());
            
            List<ChatMessage> messages = new ArrayList<>(recentEntities.size());
            for (AiLogEntity entity : recentEntities) {
                messages.add(toChatMessage(entity));
            }
            
            return messages;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Xóa conversation (xóa tất cả messages trong conversation)
     * Sử dụng TransactionTemplate để đảm bảo delete operation chạy trong transaction
     */
    public Mono<Boolean> deleteConversation(String conversationId, String userId) {
        return Mono.fromCallable(() -> {
            try {
                return transactionTemplate.execute(status -> {
                    boolean exists = aiLogRepository.existsByConversationIdAndUserId(conversationId, userId);
                    if (!exists) {
                        log.warn("Conversation not found or not owned by user: conversationId={}, userId={}", conversationId, userId);
                        return false;
                    }
                    
                    long deleted = aiLogRepository.deleteByConversationIdAndUserId(conversationId, userId);
                    log.info("Deleted conversation: conversationId={}, userId={}, deletedMessages={}", conversationId, userId, deleted);
                    return deleted > 0;
                });
            } catch (Exception e) {
                log.error("Error deleting conversation: conversationId={}, userId={}, error={}", conversationId, userId, e.getMessage(), e);
                throw new RuntimeException("Failed to delete conversation: " + e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Convert Entity sang DTO
     */
    private ChatMessage toChatMessage(AiLogEntity entity) {
        List<String> tips = new ArrayList<>();
        List<String> disclaimers = new ArrayList<>();
        
        // Parse tips và disclaimers từ sanitizedAnswer nếu có
        if (entity.getSanitizedAnswer() != null && !entity.getSanitizedAnswer().isEmpty()) {
            try {
                var jsonNode = objectMapper.readTree(entity.getSanitizedAnswer());
                if (jsonNode.has("tips") && jsonNode.get("tips").isArray()) {
                    for (var tip : jsonNode.get("tips")) {
                        if (tip.isTextual()) {
                            tips.add(tip.asText());
                        }
                    }
                }
                if (jsonNode.has("disclaimers") && jsonNode.get("disclaimers").isArray()) {
                    for (var disclaimer : jsonNode.get("disclaimers")) {
                        if (disclaimer.isTextual()) {
                            disclaimers.add(disclaimer.asText());
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse tips/disclaimers from sanitizedAnswer: {}", e.getMessage());
            }
        }
        
        ZonedDateTime createdAt = normalizeZonedDateTime(
                entity.getCreatedAt(), 
                String.valueOf(entity.getId()), 
                "messageId"
        );
        
        return ChatMessage.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .userId(entity.getUserId())
                .question(entity.getQuestion())
                .answer(entity.getFormattedAnswer() != null ? entity.getFormattedAnswer() : "")
                .tips(tips)
                .disclaimers(disclaimers)
                .model(entity.getModel())
                .promptTokens(entity.getUsagePromptTokens())
                .completionTokens(entity.getUsageCompletionTokens())
                .totalTokens(entity.getUsageTotalTokens())
                .createdAt(createdAt) // Đảm bảo không null và đúng format
                .build();
    }

    /**
     * Normalize ZonedDateTime về UTC và validate
     */
    private ZonedDateTime normalizeZonedDateTime(ZonedDateTime dateTime, String identifier, String fieldName) {
        ZonedDateTime normalized = DateTimeUtils.normalizeToUtc(dateTime, true);
        if (normalized != dateTime && dateTime != null) {
            log.warn("{} normalized for {}: {} -> {}", fieldName, identifier, dateTime, normalized);
        }
        return normalized;
    }

    /**
     * Format thời gian tương đối từ ZonedDateTime (tiếng Việt)
     * Ví dụ: "Vừa xong", "2 phút trước", "1 giờ trước", "Hôm qua", "3 ngày trước", v.v.
     */
    private String formatRelativeTime(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        ZonedDateTime now = DateTimeUtils.nowUtc();
        ZonedDateTime target = dateTime.withZoneSameInstant(UTC);
        
        Duration duration = Duration.between(target, now);
        long seconds = duration.getSeconds();
        
        if (seconds < 0 || seconds < Constants.Time.SECONDS_PER_MINUTE) {
            return "Vừa xong";
        }
        
        long minutes = seconds / Constants.Time.SECONDS_PER_MINUTE;
        if (minutes < Constants.Time.MINUTES_PER_HOUR) {
            return minutes + " phút trước";
        }
        
        long hours = minutes / Constants.Time.MINUTES_PER_HOUR;
        if (hours < Constants.Time.HOURS_PER_DAY) {
            return hours + " giờ trước";
        }
        
        LocalDate today = now.toLocalDate();
        LocalDate targetDate = target.toLocalDate();
        if (targetDate.equals(today.minusDays(1))) {
            return "Hôm qua";
        }
        
        long days = hours / Constants.Time.HOURS_PER_DAY;
        if (days < Constants.Time.DAYS_PER_WEEK) {
            return days + " ngày trước";
        }
        
        long weeks = days / Constants.Time.DAYS_PER_WEEK;
        if (weeks < 4) {
            return weeks + " tuần trước";
        }
        
        long months = days / Constants.Time.DAYS_PER_MONTH;
        if (months < Constants.Time.MONTHS_PER_YEAR) {
            return months + " tháng trước";
        }
        
        return targetDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}

