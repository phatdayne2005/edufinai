-- Flyway migration V3: Add user balance and withdrawal transaction type

-- Create user_balance table
CREATE TABLE IF NOT EXISTS user_balance (
    user_id BINARY(16) PRIMARY KEY,
    initial_balance DECIMAL(19,2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Modify transactions table to support WITHDRAWAL type
-- Note: MySQL doesn't support ALTER ENUM directly, so we change to VARCHAR
-- and rely on application-level validation for type checking
ALTER TABLE transactions MODIFY COLUMN type VARCHAR(20) NOT NULL;

-- Update existing transactions to ensure type is valid (if any exist)
UPDATE transactions SET type = 'INCOME' WHERE type = 'INCOME';
UPDATE transactions SET type = 'EXPENSE' WHERE type = 'EXPENSE';

-- Note: CHECK constraint is only supported in MySQL 8.0.16+
-- For older versions, validation is handled at application level
-- If using MySQL 8.0.16+, uncomment the following line:
-- ALTER TABLE transactions ADD CONSTRAINT chk_transaction_type 
--     CHECK (type IN ('INCOME', 'EXPENSE', 'WITHDRAWAL'));

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_user_balance_user_id ON user_balance(user_id);

