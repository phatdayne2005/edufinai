# Hướng dẫn cấu hình Gemini API Key

## Vấn đề
Lỗi: `API key not valid. Please pass a valid API key.`

## Cách 1: Set Environment Variable (Khuyến nghị)

### Windows PowerShell:
```powershell
# Set cho session hiện tại
$env:GEMINI_API_KEY = "YOUR_API_KEY_HERE"

# Set vĩnh viễn (cho user hiện tại)
[System.Environment]::SetEnvironmentVariable("GEMINI_API_KEY", "YOUR_API_KEY_HERE", "User")
```

### Windows Command Prompt:
```cmd
setx GEMINI_API_KEY "YOUR_API_KEY_HERE"
```

### Linux/Mac:
```bash
export GEMINI_API_KEY="YOUR_API_KEY_HERE"

# Hoặc thêm vào ~/.bashrc hoặc ~/.zshrc
echo 'export GEMINI_API_KEY="YOUR_API_KEY_HERE"' >> ~/.bashrc
source ~/.bashrc
```

## Cách 2: Set trong application.yaml (Không khuyến nghị cho production)

**⚠️ CẢNH BÁO: Không commit API key vào git!**

```yaml
gemini:
  apiUrl: https://generativelanguage.googleapis.com/v1beta
  apiKey: YOUR_API_KEY_HERE  # Thay YOUR_API_KEY_HERE bằng key thực
  model: gemini-2.5-flash
```

## Cách 3: Set khi chạy app

### PowerShell:
```powershell
$env:GEMINI_API_KEY = "YOUR_API_KEY_HERE"
mvn spring-boot:run
```

### Command Prompt:
```cmd
set GEMINI_API_KEY=YOUR_API_KEY_HERE
mvn spring-boot:run
```

### Linux/Mac:
```bash
GEMINI_API_KEY="YOUR_API_KEY_HERE" mvn spring-boot:run
```

## Lấy Gemini API Key

1. Truy cập: https://aistudio.google.com/app/apikey
2. Đăng nhập với Google account
3. Tạo API key mới
4. Copy API key và set vào environment variable

## Kiểm tra API Key đã được set

### Windows PowerShell:
```powershell
echo $env:GEMINI_API_KEY
```

### Windows Command Prompt:
```cmd
echo %GEMINI_API_KEY%
```

### Linux/Mac:
```bash
echo $GEMINI_API_KEY
```

## Sau khi set API key

1. **Restart app** (nếu đang chạy):
   ```powershell
   # Stop app (Ctrl+C)
   mvn spring-boot:run
   ```

2. **Test lại Chat API**:
   ```powershell
   .\test-chat.ps1
   ```

## Lưu ý

- **KHÔNG** commit API key vào git
- **KHÔNG** hardcode API key trong code
- Sử dụng environment variable hoặc secret management service
- Nếu dùng `application.yaml`, đảm bảo file này không được commit (thêm vào `.gitignore`)


