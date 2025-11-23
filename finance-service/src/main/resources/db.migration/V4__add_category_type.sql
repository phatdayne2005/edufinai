-- Flyway migration V4: Add category type (INCOME, EXPENSE, BOTH)

-- Add type column to category table
ALTER TABLE category ADD COLUMN IF NOT EXISTS type VARCHAR(10) NOT NULL DEFAULT 'EXPENSE';

-- Update existing default categories:
-- - "Khác" → type = BOTH (dùng cho cả INCOME và EXPENSE)
-- - Các category khác → type = EXPENSE (mặc định cho khoản chi)
UPDATE category 
SET type = 'BOTH' 
WHERE name = 'Khác' AND is_default = 1;

UPDATE category 
SET type = 'EXPENSE' 
WHERE name != 'Khác' AND is_default = 1;

-- Update existing user categories to EXPENSE (mặc định)
UPDATE category 
SET type = 'EXPENSE' 
WHERE is_default = 0 AND (type IS NULL OR type = '');

-- Add check constraint (MySQL 8.0.16+)
-- Note: For older MySQL versions, validation is handled at application level
-- ALTER TABLE category ADD CONSTRAINT chk_category_type 
--     CHECK (type IN ('INCOME', 'EXPENSE', 'BOTH'));

