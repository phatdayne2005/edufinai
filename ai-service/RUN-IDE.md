# Hướng Dẫn Chạy App Từ IDE

## Vấn đề thường gặp khi chạy từ IDE

### 1. **Environment Variables không được set** (Phổ biến nhất!)
- IDE không tự động load environment variables từ terminal
- Cần set trong Run Configuration của IDE

### 2. **Java version không đúng**
- App cần Java 21
- IDE có thể dùng Java version khác

### 3. **Maven dependencies chưa được download**
- Cần refresh Maven project

---

## IntelliJ IDEA

### Bước 1: Kiểm tra Java Version

1. **File → Project Structure → Project**
   - **SDK**: Chọn Java 21
   - **Language level**: 21

2. **File → Project Structure → Modules**
   - **Language level**: 21

### Bước 2: Refresh Maven

1. **View → Tool Windows → Maven**
2. Click icon **Reload All Maven Projects** (🔄)
3. Hoặc: **Right-click `pom.xml` → Maven → Reload Project**

### Bước 3: Cấu hình Run Configuration

1. **Run → Edit Configurations...**
2. Tìm hoặc tạo configuration cho `AiServiceApplication`
3. Trong tab **Environment variables**, thêm:
   ```
   GEMINI_API_KEY=your-api-key-here
   ```
4. Hoặc click **Environment variables** → **+** → Thêm:
   - **Name**: `GEMINI_API_KEY`
   - **Value**: `your-api-key-here`

### Bước 4: Cấu hình VM Options (nếu cần)

Trong **Run → Edit Configurations → VM options**, thêm:
```
-Dfile.encoding=UTF-8
```

### Bước 5: Chạy App

1. Mở file `AiServiceApplication.java`
2. Click nút **Run** ▶️ bên cạnh `main` method
3. Hoặc: **Right-click → Run 'AiServiceApplication.main()'**

---

## Eclipse / Spring Tool Suite

### Bước 1: Kiểm tra Java Version

1. **Project → Properties → Java Build Path → Libraries**
2. Đảm bảo **JRE System Library** là Java 21

### Bước 2: Refresh Maven

1. **Right-click project → Maven → Update Project...**
2. Check **Force Update of Snapshots/Releases**
3. Click **OK**

### Bước 3: Cấu hình Run Configuration

1. **Run → Run Configurations...**
2. **Right-click Java Application → New**
3. Đặt tên: `AiServiceApplication`
4. **Main class**: `vn.uth.edufinai.AiServiceApplication`
5. Tab **Environment**:
   - Click **New**
   - **Name**: `GEMINI_API_KEY`
   - **Value**: `your-api-key-here`
   - Click **OK**

### Bước 4: Chạy App

1. **Run → Run Configurations...**
2. Chọn `AiServiceApplication`
3. Click **Run**

---

## VS Code

### Bước 1: Cài đặt Extensions

- **Extension Pack for Java** (Microsoft)
- **Spring Boot Extension Pack** (VMware)

### Bước 2: Cấu hình Java

1. **File → Preferences → Settings**
2. Tìm `java.configuration.runtimes`
3. Thêm Java 21:
   ```json
   {
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-21",
         "path": "C:/path/to/java-21"
       }
     ]
   }
   ```

### Bước 3: Tạo `.vscode/launch.json`

Tạo file `.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Run AiServiceApplication",
      "request": "launch",
      "mainClass": "vn.uth.edufinai.AiServiceApplication",
      "projectName": "edufinai-service",
      "env": {
        "GEMINI_API_KEY": "your-api-key-here"
      },
      "vmArgs": "-Dfile.encoding=UTF-8"
    }
  ]
}
```

### Bước 4: Chạy App

1. Mở file `AiServiceApplication.java`
2. Click **Run** ▶️ bên trên `main` method
3. Hoặc: **F5** (Debug) hoặc **Ctrl+F5** (Run)

---

## Kiểm tra sau khi chạy

### 1. Xem Console Log

Tìm dòng:
```
Started AiServiceApplication in X.XXX seconds
```

### 2. Test API

```powershell
# Test Chat API
Invoke-RestMethod -Uri "http://localhost:8080/api/chat/ask" `
    -Method POST `
    -ContentType "application/json" `
    -Body '{"userId":"test","question":"test"}'
```

---

## Lỗi thường gặp và cách fix

### ❌ Lỗi: "GEMINI_API_KEY is not configured"

**Fix:**
- Set environment variable trong Run Configuration của IDE
- Hoặc set trong System Environment Variables (Windows)

### ❌ Lỗi: "Java version mismatch"

**Fix:**
- Đảm bảo IDE dùng Java 21
- Check: **File → Project Structure → Project → SDK**

### ❌ Lỗi: "Cannot connect to MySQL"

**Fix:**
- Đảm bảo MySQL đang chạy
- Check connection trong `application.yaml`

### ❌ Lỗi: "Port 8080 already in use"

**Fix:**
- Đổi port trong `application.yaml`:
  ```yaml
  server:
    port: 8081
  ```
- Hoặc kill process đang dùng port 8080:
  ```powershell
  # Windows
  netstat -ano | findstr :8080
  taskkill /PID <PID> /F
  ```

### ❌ Lỗi: "Maven dependencies not found"

**Fix:**
- Refresh Maven project trong IDE
- Hoặc chạy: `mvn clean install` từ terminal

---

## Cách nhanh nhất: Dùng Run Configuration với Environment Variable

### IntelliJ IDEA:

1. **Run → Edit Configurations...**
2. Tìm `AiServiceApplication`
3. Tab **Environment variables**:
   ```
   GEMINI_API_KEY=your-api-key-here
   ```
4. Click **OK**
5. Chạy app

### Eclipse:

1. **Run → Run Configurations...**
2. Tab **Environment**
3. Add: `GEMINI_API_KEY=your-api-key-here`
4. Click **Run**

---

## Lưu ý

- **KHÔNG** commit API key vào git
- Nếu dùng System Environment Variables, restart IDE sau khi set
- Có thể tạo file `.env` và load bằng plugin (nếu IDE hỗ trợ)

---

## Test nhanh

Sau khi chạy app từ IDE, test bằng:

```powershell
.\test-chat.ps1
```

Nếu thành công, bạn sẽ thấy response từ Gemini API! ✅


