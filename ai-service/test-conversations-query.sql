-- Test query để lấy danh sách conversations
-- Thay 'anonymous' bằng userId thực tế của bạn

USE ai_service;

-- Test query với userId = 'anonymous'
SELECT 
    conversation_id as conversationId, 
    MAX(created_at) as lastUpdated
FROM ai_logs
WHERE user_id = 'anonymous' AND conversation_id IS NOT NULL
GROUP BY conversation_id
ORDER BY MAX(created_at) DESC;

-- Test query với userId cụ thể (thay 'user123' bằng userId thực tế)
-- SELECT 
--     conversation_id as conversationId, 
--     MAX(created_at) as lastUpdated
-- FROM ai_logs
-- WHERE user_id = 'user123' AND conversation_id IS NOT NULL
-- GROUP BY conversation_id
-- ORDER BY MAX(created_at) DESC;

-- Xem tất cả conversations (không filter theo user)
-- SELECT 
--     conversation_id as conversationId, 
--     user_id as userId,
--     MAX(created_at) as lastUpdated
-- FROM ai_logs
-- WHERE conversation_id IS NOT NULL
-- GROUP BY conversation_id, user_id
-- ORDER BY MAX(created_at) DESC;

