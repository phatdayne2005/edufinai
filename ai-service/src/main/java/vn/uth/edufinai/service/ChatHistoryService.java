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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
            // Sử dụng UTC để nhất quán
            entity.setCreatedAt(ZonedDateTime.now(java.time.ZoneId.of("UTC")));
            
            return aiLogRepository.save(entity);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Lấy lịch sử conversation
     */
    public Mono<ConversationHistory> getConversationHistory(String conversationId) {
        return Mono.fromCallable(() -> {
            List<AiLogEntity> entities = aiLogRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
            
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
                    .userId(entities.get(0).getUserId())
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
                
                // Convert Timestamp sang ZonedDateTime (sử dụng UTC để nhất quán)
                ZonedDateTime lastUpdated = null;
                if (conv.getLastUpdated() != null) {
                    try {
                        lastUpdated = conv.getLastUpdated().toInstant().atZone(java.time.ZoneId.of("UTC"));
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
                
                Optional<AiLogEntity> firstMessage = aiLogRepository.findFirstByConversationIdOrderByCreatedAtAsc(conversationId);
                long messageCount = aiLogRepository.countByConversationId(conversationId);
                
                if (firstMessage.isPresent()) {
                    String title = firstMessage.get().getQuestion();
                    if (title != null && title.length() > 100) {
                        title = title.substring(0, 100) + "...";
                    }
                    
                    // Đảm bảo createdAt cũng dùng UTC và không null
                    ZonedDateTime createdAt = firstMessage.get().getCreatedAt();
                    if (createdAt == null) {
                        log.warn("createdAt is null for conversationId={}, using current time as fallback", conversationId);
                        createdAt = ZonedDateTime.now(java.time.ZoneId.of("UTC"));
                    } else {
                        // Validate: đảm bảo timestamp không phải epoch (1970-01-01)
                        if (createdAt.toEpochSecond() <= 0) {
                            log.warn("Invalid createdAt timestamp (epoch) for conversationId={}, using current time as fallback", conversationId);
                            createdAt = ZonedDateTime.now(java.time.ZoneId.of("UTC"));
                        } else if (!createdAt.getZone().equals(java.time.ZoneId.of("UTC"))) {
                            createdAt = createdAt.withZoneSameInstant(java.time.ZoneId.of("UTC"));
                        }
                    }
                    
                    // Nếu lastUpdated null hoặc invalid, dùng createdAt
                    if (lastUpdated == null) {
                        lastUpdated = createdAt;
                    }
                    
                    // Tính relative time từ updatedAt (hoặc createdAt nếu updatedAt null)
                    String relativeTimeStr = formatRelativeTime(lastUpdated != null ? lastUpdated : createdAt);
                    
                    // Log để debug (có thể remove sau)
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
     */
    public Mono<List<ChatMessage>> getConversationContext(String conversationId, int limit) {
        return Mono.fromCallable(() -> {
            List<AiLogEntity> entities = aiLogRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
            
            // Lấy N messages gần nhất
            int start = Math.max(0, entities.size() - limit);
            List<AiLogEntity> recentEntities = entities.subList(start, entities.size());
            
            List<ChatMessage> messages = new ArrayList<>();
            for (AiLogEntity entity : recentEntities) {
                ChatMessage message = toChatMessage(entity);
                messages.add(message);
            }
            
            return messages;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Xóa conversation (xóa tất cả messages trong conversation)
     * Sử dụng TransactionTemplate để đảm bảo delete operation chạy trong transaction
     */
    public Mono<Boolean> deleteConversation(String conversationId) {
        return Mono.fromCallable(() -> {
            try {
                return transactionTemplate.execute(status -> {
                    // Kiểm tra conversation có tồn tại không
                    long count = aiLogRepository.countByConversationId(conversationId);
                    if (count == 0) {
                        log.warn("Conversation not found: conversationId={}", conversationId);
                        return false;
                    }
                    
                    // Xóa tất cả messages trong conversation
                    aiLogRepository.deleteByConversationId(conversationId);
                    log.info("Deleted conversation: conversationId={}, messageCount={}", conversationId, count);
                    return true;
                });
            } catch (Exception e) {
                log.error("Error deleting conversation: conversationId={}, error={}", conversationId, e.getMessage(), e);
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
        
        // Đảm bảo createdAt không null và đúng format
        ZonedDateTime createdAt = entity.getCreatedAt();
        if (createdAt == null) {
            log.warn("createdAt is null for messageId={}, using current time as fallback", entity.getId());
            createdAt = ZonedDateTime.now(java.time.ZoneId.of("UTC"));
        } else {
            // Validate: đảm bảo timestamp không phải epoch (1970-01-01)
            if (createdAt.toEpochSecond() <= 0) {
                log.warn("Invalid createdAt timestamp (epoch) for messageId={}, using current time as fallback", entity.getId());
                createdAt = ZonedDateTime.now(java.time.ZoneId.of("UTC"));
            } else if (!createdAt.getZone().equals(java.time.ZoneId.of("UTC"))) {
                createdAt = createdAt.withZoneSameInstant(java.time.ZoneId.of("UTC"));
            }
        }
        
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
     * Format thời gian tương đối từ ZonedDateTime (tiếng Việt)
     * Ví dụ: "Vừa xong", "2 phút trước", "1 giờ trước", "Hôm qua", "3 ngày trước", v.v.
     */
    private String formatRelativeTime(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        // Chuyển về UTC để tính toán
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime target = dateTime.withZoneSameInstant(ZoneId.of("UTC"));
        
        Duration duration = Duration.between(target, now);
        long seconds = duration.getSeconds();
        
        // Nếu thời gian trong tương lai (có thể do lệch thời gian server)
        if (seconds < 0) {
            return "Vừa xong";
        }
        
        // Dưới 1 phút
        if (seconds < 60) {
            return "Vừa xong";
        }
        
        // Dưới 1 giờ
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " phút trước";
        }
        
        // Dưới 24 giờ
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " giờ trước";
        }
        
        // Kiểm tra hôm qua
        LocalDate today = now.toLocalDate();
        LocalDate targetDate = target.toLocalDate();
        LocalDate yesterday = today.minusDays(1);
        
        if (targetDate.equals(yesterday)) {
            return "Hôm qua";
        }
        
        // Dưới 7 ngày
        long days = hours / 24;
        if (days < 7) {
            return days + " ngày trước";
        }
        
        // Dưới 4 tuần
        long weeks = days / 7;
        if (weeks < 4) {
            return weeks + " tuần trước";
        }
        
        // Dưới 12 tháng
        long months = days / 30;
        if (months < 12) {
            return months + " tháng trước";
        }
        
        // Quá 1 năm - hiển thị ngày tháng đầy đủ
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return targetDate.format(formatter);
    }
}

