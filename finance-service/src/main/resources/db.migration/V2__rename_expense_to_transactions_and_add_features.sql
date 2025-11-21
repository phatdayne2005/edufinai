-- Flyway migration V2: Rename expense to transactions and add new features (MySQL)

-- Rename expense table to transactions and primary key column
ALTER TABLE expense RENAME TO transactions;
ALTER TABLE transactions CHANGE COLUMN expense_id transaction_id BINARY(16) NOT NULL;

-- Add transaction_date and name fields to transactions
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS transaction_date DATETIME;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS name VARCHAR(255);

-- Update existing records: set transaction_date = created_at if null and enforce NOT NULL
UPDATE transactions SET transaction_date = created_at WHERE transaction_date IS NULL;
ALTER TABLE transactions MODIFY COLUMN transaction_date DATETIME NOT NULL;

-- Update foreign key constraint name (MySQL)
ALTER TABLE transactions DROP FOREIGN KEY fk_expense_goal;
ALTER TABLE transactions ADD CONSTRAINT fk_transaction_goal
    FOREIGN KEY (goal_id) REFERENCES goal(goal_id);

-- Add saved_amount to goal table
ALTER TABLE goal ADD COLUMN IF NOT EXISTS saved_amount DECIMAL(19,2) NOT NULL DEFAULT 0;

-- Create category table
CREATE TABLE IF NOT EXISTS category (
    category_id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_category_user_name (user_id, name)
);

-- Insert default categories (ignore if already exist)
INSERT IGNORE INTO category (category_id, user_id, name, is_default, created_at) VALUES
    (UUID_TO_BIN(UUID()), UUID_TO_BIN('00000000-0000-0000-0000-000000000000'), 'Ăn uống', 1, CURRENT_TIMESTAMP),
    (UUID_TO_BIN(UUID()), UUID_TO_BIN('00000000-0000-0000-0000-000000000000'), 'Mua sắm', 1, CURRENT_TIMESTAMP),
    (UUID_TO_BIN(UUID()), UUID_TO_BIN('00000000-0000-0000-0000-000000000000'), 'Giải trí', 1, CURRENT_TIMESTAMP),
    (UUID_TO_BIN(UUID()), UUID_TO_BIN('00000000-0000-0000-0000-000000000000'), 'Nhà ở', 1, CURRENT_TIMESTAMP),
    (UUID_TO_BIN(UUID()), UUID_TO_BIN('00000000-0000-0000-0000-000000000000'), 'Di chuyển', 1, CURRENT_TIMESTAMP),
    (UUID_TO_BIN(UUID()), UUID_TO_BIN('00000000-0000-0000-0000-000000000000'), 'Khác', 1, CURRENT_TIMESTAMP);

-- Add category_id column to transactions and foreign key
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS category_id BINARY(16);
ALTER TABLE transactions ADD CONSTRAINT fk_transaction_category
    FOREIGN KEY (category_id) REFERENCES category(category_id);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_goal_id ON transactions(goal_id);
CREATE INDEX IF NOT EXISTS idx_transactions_transaction_date ON transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_category_user_id ON category(user_id);

