-- Migration script để thêm conversation history features
-- Chạy script này trong MySQL để cập nhật database schema

USE ai_service;

-- Thêm column conversation_id nếu chưa có
ALTER TABLE ai_logs 
ADD COLUMN IF NOT EXISTS conversation_id VARCHAR(64) NULL AFTER id;

-- Thêm column formatted_answer nếu chưa có
ALTER TABLE ai_logs 
ADD COLUMN IF NOT EXISTS formatted_answer LONGTEXT NULL AFTER sanitized_answer;

-- Thêm index cho conversation_id nếu chưa có
CREATE INDEX IF NOT EXISTS idx_ai_logs_conversation ON ai_logs(conversation_id, created_at);

-- Kiểm tra kết quả
SELECT 'Migration completed successfully!' AS result;
DESCRIBE ai_logs;

