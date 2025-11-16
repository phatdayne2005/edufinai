-- Migration script để thêm conversation history features (Manual version)
-- Sử dụng script này nếu MySQL không hỗ trợ IF NOT EXISTS

USE ai_service;

-- Kiểm tra và thêm conversation_id
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'ai_service' 
    AND TABLE_NAME = 'ai_logs' 
    AND COLUMN_NAME = 'conversation_id'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ai_logs ADD COLUMN conversation_id VARCHAR(64) NULL AFTER id',
    'SELECT "conversation_id column already exists" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Kiểm tra và thêm formatted_answer
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'ai_service' 
    AND TABLE_NAME = 'ai_logs' 
    AND COLUMN_NAME = 'formatted_answer'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ai_logs ADD COLUMN formatted_answer LONGTEXT NULL AFTER sanitized_answer',
    'SELECT "formatted_answer column already exists" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Kiểm tra và thêm index
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'ai_service' 
    AND TABLE_NAME = 'ai_logs' 
    AND INDEX_NAME = 'idx_ai_logs_conversation'
);

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_ai_logs_conversation ON ai_logs(conversation_id, created_at)',
    'SELECT "idx_ai_logs_conversation index already exists" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Kiểm tra kết quả
SELECT 'Migration completed successfully!' AS result;
DESCRIBE ai_logs;
SHOW INDEXES FROM ai_logs;

