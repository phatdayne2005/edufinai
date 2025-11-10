CREATE TABLE refresh_tokens (
                                token_id BINARY(16) PRIMARY KEY DEFAULT (UUID_TO_BIN(UUID())),
                                user_id BINARY(16) NOT NULL,
                                token VARCHAR(500) NOT NULL,
                                expires_at DATETIME(6) NOT NULL,
                                is_revoked BOOLEAN DEFAULT FALSE,
                                created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),

                                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                INDEX idx_token (token),
                                INDEX idx_user_id (user_id),
                                INDEX idx_expires_at (expires_at)
);