CREATE TABLE IF NOT EXISTS ai_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id VARCHAR(64),
  user_id VARCHAR(64),
  question LONGTEXT,
  prompt LONGTEXT,
  model VARCHAR(100),
  raw_answer LONGTEXT,
  sanitized_answer LONGTEXT,
  formatted_answer LONGTEXT,
  usage_prompt_tokens INT NULL,
  usage_completion_tokens INT NULL,
  usage_total_tokens INT NULL,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ai_logs_user_time (user_id, created_at),
  INDEX idx_ai_logs_conversation (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
