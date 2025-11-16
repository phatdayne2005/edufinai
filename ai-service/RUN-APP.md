# Hướng Dẫn Chạy App

## 1. Chuẩn bị trước khi chạy

### Yêu cầu:
- ✅ Java 21
- ✅ Maven 3.6+
- ✅ MySQL đang chạy
- ✅ Database `ai_service` đã tạo
- ✅ Gemini API Key

### Bước 1: Tạo Database
```sql
CREATE DATABASE IF NOT EXISTS ai_service;
```

### Bước 2: Set Gemini API Key
```powershell
# Windows PowerShell
$env:GEMINI_API_KEY="your-api-key-here"

# Hoặc set trong System Environment Variables
```

### Bước 3: Kiểm tra MySQL đang chạy
```powershell
# Test connection
mysql -u root -p -e "SHOW DATABASES;"
```

---

## 2. Cách chạy app

### Cách 1: Chạy bằng Maven (Khuyến nghị)
```bash
# Từ thư mục project
mvn spring-boot:run
```

### Cách 2: Chạy bằng IDE
**⚠️ QUAN TRỌNG: Cần set Environment Variable `GEMINI_API_KEY` trong Run Configuration!**

Xem hướng dẫn chi tiết: [RUN-IDE.md](RUN-IDE.md)

**Tóm tắt:**
1. Mở project trong IntelliJ IDEA / Eclipse / VS Code
2. **Set Environment Variable `GEMINI_API_KEY` trong Run Configuration**
3. Tìm file `AiServiceApplication.java`
4. Right-click → Run 'AiServiceApplication.main()'
5. Hoặc click nút Run ▶️

### Cách 3: Build JAR rồi chạy
```bash
# Build
mvn clean package

# Chạy
java -jar target/edufinai-service-0.0.1-SNAPSHOT.jar
```

---

## 3. Kiểm tra app đã chạy

### Check logs:
```
Started AiServiceApplication in X.XXX seconds
```

### Test endpoint health:
```powershell
# Test xem app có chạy không
Invoke-WebRequest -Uri "http://localhost:8080/api/chat/ask" -Method POST -ContentType "application/json" -Body '{"question":"test"}'
```

---

## 4. Test sau khi chạy

### Test Chat API:
```powershell
.\test-chat.ps1
```

### Test Report API:
```powershell
.\test-report.ps1
```

---

## 5. Lỗi thường gặp khi chạy

### Lỗi: "Cannot connect to MySQL"
- **Fix**: Đảm bảo MySQL đang chạy và database `ai_service` đã tạo

### Lỗi: "Gemini API key is empty"
- **Fix**: Set `GEMINI_API_KEY` environment variable

### Lỗi: "Port 8080 already in use"
- **Fix**: Đổi port trong `application.yaml` hoặc kill process đang dùng port 8080

### Lỗi: "Java version mismatch"
- **Fix**: Cần Java 21, check bằng: `java -version`

