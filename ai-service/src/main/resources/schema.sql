CREATE TABLE IF NOT EXISTS ai_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  report_date DATE NOT NULL UNIQUE,
  model VARCHAR(100),
  prompt LONGTEXT NULL,
  raw_summary LONGTEXT NULL,
  sanitized_summary LONGTEXT NOT NULL,
  usage_prompt_tokens INT NULL,
  usage_completion_tokens INT NULL,
  usage_total_tokens INT NULL,
  metadata_json LONGTEXT NULL,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
