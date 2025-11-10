ALTER TABLE users
    ADD COLUMN display_name VARCHAR(50) NOT NULL DEFAULT 'User',
ADD COLUMN preferences JSON;

-- Update existing records to set display_name from username
UPDATE users SET display_name = username WHERE display_name = 'User';

-- Add index for display_name
CREATE INDEX idx_display_name ON users(display_name);