# Migration Guide - Conversation History Feature

## Vấn đề

Lỗi: `Unknown column 'conversation_id' in 'field list'`

Lỗi này xảy ra vì database chưa có các columns mới được thêm vào để hỗ trợ tính năng conversation history.

## Giải pháp

Cần chạy migration script để thêm các columns sau vào table `ai_logs`:
- `conversation_id` (VARCHAR(64))
- `formatted_answer` (LONGTEXT)
- Index `idx_ai_logs_conversation`

---

## Cách 1: Chạy Migration Script (Khuyến nghị)

### Bước 1: Mở MySQL Client

```bash
mysql -u root -p
```

### Bước 2: Chọn database

```sql
USE ai_service;
```

### Bước 3: Chạy migration script

**Nếu MySQL hỗ trợ IF NOT EXISTS (MySQL 8.0.19+):**
```bash
mysql -u root -p ai_service < migration-add-conversation-history.sql
```

**Hoặc copy nội dung từ file `migration-add-conversation-history.sql` và paste vào MySQL client**

**Nếu MySQL không hỗ trợ IF NOT EXISTS:**
```bash
mysql -u root -p ai_service < migration-add-conversation-history-manual.sql
```

---

## Cách 2: Chạy thủ công từng lệnh

### Bước 1: Mở MySQL Client

```bash
mysql -u root -p
```

### Bước 2: Chọn database

```sql
USE ai_service;
```

### Bước 3: Chạy từng lệnh sau

```sql
-- Thêm column conversation_id
ALTER TABLE ai_logs ADD COLUMN conversation_id VARCHAR(64) NULL AFTER id;

-- Thêm column formatted_answer
ALTER TABLE ai_logs ADD COLUMN formatted_answer LONGTEXT NULL AFTER sanitized_answer;

-- Thêm index
CREATE INDEX idx_ai_logs_conversation ON ai_logs(conversation_id, created_at);
```

**Lưu ý:** Nếu column/index đã tồn tại, MySQL sẽ báo lỗi. Bạn có thể bỏ qua lỗi đó hoặc kiểm tra trước:

```sql
-- Kiểm tra columns hiện có
DESCRIBE ai_logs;

-- Kiểm tra indexes hiện có
SHOW INDEXES FROM ai_logs;
```

---

## Cách 3: Sử dụng PowerShell Script (Windows)

Tạo file `migrate-database.ps1`:

```powershell
$dbName = "ai_service"
$dbUser = "root"
$dbPassword = "root"  # Thay đổi nếu cần

Write-Host "=== Running Migration ===" -ForegroundColor Green

$sql = @"
USE $dbName;

ALTER TABLE ai_logs ADD COLUMN conversation_id VARCHAR(64) NULL AFTER id;
ALTER TABLE ai_logs ADD COLUMN formatted_answer LONGTEXT NULL AFTER sanitized_answer;
CREATE INDEX idx_ai_logs_conversation ON ai_logs(conversation_id, created_at);
"@

$sql | mysql -u $dbUser -p$dbPassword $dbName

Write-Host "Migration completed!" -ForegroundColor Green
```

Chạy:
```powershell
.\migrate-database.ps1
```

---

## Kiểm tra sau khi migration

### 1. Kiểm tra columns

```sql
DESCRIBE ai_logs;
```

Kết quả mong đợi:
- Có column `conversation_id` (VARCHAR(64))
- Có column `formatted_answer` (LONGTEXT)

### 2. Kiểm tra indexes

```sql
SHOW INDEXES FROM ai_logs;
```

Kết quả mong đợi:
- Có index `idx_ai_logs_conversation` trên `(conversation_id, created_at)`

### 3. Test API

Sau khi migration xong, restart application và test lại:

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "question": "hello"
  }'
```

---

## Rollback (Nếu cần)

Nếu muốn rollback migration:

```sql
USE ai_service;

-- Xóa index
DROP INDEX idx_ai_logs_conversation ON ai_logs;

-- Xóa columns
ALTER TABLE ai_logs DROP COLUMN formatted_answer;
ALTER TABLE ai_logs DROP COLUMN conversation_id;
```

**⚠️ CẢNH BÁO:** Rollback sẽ xóa dữ liệu trong các columns này!

---

## Lưu ý

1. **Backup database trước khi migration:**
   ```bash
   mysqldump -u root -p ai_service > ai_service_backup.sql
   ```

2. **Nếu có dữ liệu cũ:** Các records cũ sẽ có `conversation_id = NULL`. Điều này không ảnh hưởng đến hoạt động, nhưng chúng sẽ không được nhóm vào conversation.

3. **Performance:** Index mới sẽ giúp query conversation history nhanh hơn.

---

## Troubleshooting

### Lỗi: "Column already exists"
- Có nghĩa là column đã được thêm trước đó
- Bạn có thể bỏ qua lỗi này hoặc kiểm tra bằng `DESCRIBE ai_logs`

### Lỗi: "Duplicate key name"
- Có nghĩa là index đã tồn tại
- Bạn có thể bỏ qua lỗi này hoặc kiểm tra bằng `SHOW INDEXES FROM ai_logs`

### Lỗi: "Table doesn't exist"
- Đảm bảo database `ai_service` đã được tạo
- Chạy: `CREATE DATABASE IF NOT EXISTS ai_service;`

---

**Sau khi migration xong, restart application và test lại!**

