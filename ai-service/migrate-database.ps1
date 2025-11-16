# PowerShell script để migrate database - Thêm conversation history columns
# Yêu cầu: MySQL client phải được cài đặt và có trong PATH

$dbName = "ai_service"
$dbUser = "root"
$dbPassword = "root"  # Thay đổi nếu cần

Write-Host "=== Running Database Migration ===" -ForegroundColor Green
Write-Host "Database: $dbName" -ForegroundColor Cyan
Write-Host "User: $dbUser" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra MySQL connection
Write-Host "Checking MySQL connection..." -ForegroundColor Yellow
try {
    $testResult = mysql -u $dbUser -p$dbPassword -e "USE $dbName; SELECT 1;" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ MySQL connection failed!" -ForegroundColor Red
        Write-Host "Please check:" -ForegroundColor Yellow
        Write-Host "  1. MySQL is running" -ForegroundColor White
        Write-Host "  2. Database '$dbName' exists" -ForegroundColor White
        Write-Host "  3. Username and password are correct" -ForegroundColor White
        exit 1
    }
    Write-Host "✅ MySQL connection OK" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Running migration..." -ForegroundColor Yellow

# SQL commands
$sqlCommands = @"
USE $dbName;

-- Kiểm tra và thêm conversation_id
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = '$dbName' 
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
    WHERE TABLE_SCHEMA = '$dbName' 
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
    WHERE TABLE_SCHEMA = '$dbName' 
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

SELECT 'Migration completed successfully!' AS result;
"@

# Chạy SQL
try {
    $sqlCommands | mysql -u $dbUser -p$dbPassword $dbName 2>&1 | Out-Null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ Migration completed successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Verifying migration..." -ForegroundColor Yellow
        
        # Verify columns
        $verifySql = "USE $dbName; DESCRIBE ai_logs;"
        $result = mysql -u $dbUser -p$dbPassword $dbName -e $verifySql 2>&1
        
        if ($result -match "conversation_id" -and $result -match "formatted_answer") {
            Write-Host "✅ Columns verified: conversation_id, formatted_answer" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Warning: Could not verify columns. Please check manually." -ForegroundColor Yellow
        }
        
        Write-Host ""
        Write-Host "Next steps:" -ForegroundColor Cyan
        Write-Host "  1. Restart your application" -ForegroundColor White
        Write-Host "  2. Test the API again" -ForegroundColor White
    } else {
        Write-Host "❌ Migration failed!" -ForegroundColor Red
        Write-Host "Please check the error messages above." -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "❌ Error running migration: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please run the SQL commands manually:" -ForegroundColor Yellow
    Write-Host "  1. Open MySQL client: mysql -u root -p" -ForegroundColor Cyan
    Write-Host "  2. Run: USE $dbName;" -ForegroundColor Cyan
    Write-Host "  3. Copy and paste commands from: migration-add-conversation-history-manual.sql" -ForegroundColor Cyan
    exit 1
}

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Green

