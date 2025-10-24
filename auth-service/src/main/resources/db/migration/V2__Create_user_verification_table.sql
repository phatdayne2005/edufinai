CREATE TABLE user_verification (
    verification_id BINARY(16) PRIMARY KEY DEFAULT (UUID_TO_BIN(UUID())),
    user_id BINARY(16) NOT NULL,
    verification_type ENUM('EMAIL', 'PHONE', 'OTP') NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_type (user_id, verification_type),
    INDEX idx_expires_at (expires_at)
);