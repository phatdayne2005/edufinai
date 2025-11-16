# Hướng Dẫn Test App

## 1. Test Chat API - POST /api/chat/ask

### Request:
```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "question": "Tôi nên tiết kiệm như thế nào?"
  }'
```

### Hoặc dùng PowerShell:
```powershell
$body = @{
    userId = "user123"
    question = "Tôi nên tiết kiệm như thế nào?"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/chat/ask" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body
```

### Response mong đợi:
```json
{
  "userId": "user123",
  "question": "Tôi nên tiết kiệm như thế nào?",
  "answerJson": "{\"answer\":\"...\",\"tips\":[...]}",
  "model": "gemini-2.5-flash",
  "promptTokens": 150,
  "completionTokens": 200,
  "totalTokens": 350,
  "createdAt": "2024-01-01T10:00:00Z"
}
```

### Test cases:
1. ✅ Test với question hợp lệ
2. ✅ Test với userId = null (sẽ dùng "anonymous")
3. ❌ Test với question = "" (sẽ báo validation error)
4. ❌ Test với question = null (sẽ báo validation error)

---

## 2. Test Daily Report API - GET /api/reports/daily

### Request:
```bash
# Lấy report hôm nay
curl http://localhost:8080/api/reports/daily

# Lấy report theo ngày cụ thể
curl "http://localhost:8080/api/reports/daily?date=2024-01-01"
```

### Hoặc dùng PowerShell:
```powershell
# Lấy report hôm nay
Invoke-RestMethod -Uri "http://localhost:8080/api/reports/daily" -Method GET

# Lấy report theo ngày
Invoke-RestMethod -Uri "http://localhost:8080/api/reports/daily?date=2024-01-01" -Method GET
```

### Response mong đợi:
```json
{
  "reportDate": "2024-01-01",
  "model": "gemini-2.5-flash",
  "rawSummary": "...",
  "sanitizedSummary": "...",
  "usagePromptTokens": 500,
  "usageCompletionTokens": 300,
  "usageTotalTokens": 800,
  "createdAt": "2024-01-01T02:15:00Z",
  "updatedAt": "2024-01-01T02:15:00Z"
}
```

### Test cases:
1. ✅ Test với date hợp lệ (có data trong DB)
2. ❌ Test với date không có data (sẽ báo 404)
3. ✅ Test không có date param (lấy hôm nay)

---

## 3. Test Scheduler

Scheduler tự động chạy lúc 2:15 AM mỗi ngày.

### Để test thủ công:
1. Sửa cron trong `application.yaml` thành: `0 * * * * *` (chạy mỗi phút)
2. Restart app
3. Đợi 1 phút → check logs xem có chạy không
4. Check database xem có report mới không

---

## 4. Kiểm tra Logs

### Xem logs:
```bash
# Linux/Mac
tail -f logs/ai-service.log

# Windows PowerShell
Get-Content logs/ai-service.log -Wait -Tail 50
```

### Logs quan trọng:
- `[DAILY] Scheduler started` - Scheduler đã chạy
- `Chat request` - Có request chat
- `Gemini API HTTP error` - Gemini API lỗi
- `Downstream service unavailable` - Service khác không available

---

## 5. Kiểm tra Database

### Kiểm tra reports:
```sql
SELECT * FROM ai_reports ORDER BY created_at DESC LIMIT 10;
```

### Kiểm tra logs:
```sql
SELECT * FROM ai_logs ORDER BY created_at DESC LIMIT 10;
```

---

## 6. Test với Postman

### Import collection:
1. Tạo request mới
2. Method: POST
3. URL: `http://localhost:8080/api/chat/ask`
4. Headers: `Content-Type: application/json`
5. Body (raw JSON):
```json
{
  "userId": "test-user",
  "question": "Tôi có bao nhiêu tiền trong tài khoản?"
}
```

---

## 7. Lỗi thường gặp

### Lỗi: "Gemini API key is empty"
- **Nguyên nhân**: Chưa set `GEMINI_API_KEY` environment variable
- **Fix**: Set `GEMINI_API_KEY` trước khi chạy app

### Lỗi: "Downstream service unavailable"
- **Nguyên nhân**: Service khác không chạy hoặc URL sai
- **Fix**: Check URL trong `application.yaml` và đảm bảo services đang chạy

### Lỗi: "Report not found"
- **Nguyên nhân**: Chưa có report cho ngày đó
- **Fix**: Chờ scheduler chạy hoặc tạo report thủ công

### Lỗi: Validation error
- **Nguyên nhân**: `question` bị blank hoặc null
- **Fix**: Đảm bảo gửi `question` hợp lệ


