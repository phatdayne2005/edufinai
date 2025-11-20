# Hướng Dẫn Cấu Hình IP Linh Hoạt

## 📋 Tổng Quan

Bạn có thể dễ dàng chuyển đổi giữa `127.0.0.1` (localhost) và `192.168.1.9` (LAN IP) mà **không cần sửa code**!

---

## ✅ Đã Cấu Hình

Tất cả services (auth-service, ai-service, gateway) đã được cấu hình để:
- **Mặc định:** Dùng `127.0.0.1` (localhost)
- **Có thể override:** Bằng biến môi trường `EUREKA_INSTANCE_IP`

---

## 🚀 Cách Sử Dụng

### Cách 1: Dùng `127.0.0.1` (Local Development) - Mặc Định

**Không cần làm gì cả!** Chạy service như bình thường:

```powershell
# Trong IntelliJ hoặc terminal
cd auth-service
./mvnw spring-boot:run
```

→ Service sẽ tự động dùng `127.0.0.1`

---

### Cách 2: Dùng `192.168.1.9` (LAN IP) - Khi Cần Truy Cập Từ Máy Khác

**Chỉ cần set biến môi trường trước khi chạy:**

#### Windows PowerShell:
```powershell
# Set biến môi trường
$env:EUREKA_INSTANCE_IP="192.168.1.9"
$env:EUREKA_INSTANCE_HOSTNAME="localhost"

# Chạy service
cd auth-service
./mvnw spring-boot:run
```

#### Windows CMD:
```cmd
set EUREKA_INSTANCE_IP=192.168.1.9
set EUREKA_INSTANCE_HOSTNAME=localhost
cd auth-service
mvnw spring-boot:run
```

#### Linux/macOS:
```bash
export EUREKA_INSTANCE_IP=192.168.1.9
export EUREKA_INSTANCE_HOSTNAME=localhost
cd auth-service
./mvnw spring-boot:run
```

---

## 🔄 Chuyển Đổi Nhanh

### Từ `127.0.0.1` → `192.168.1.9`:

**Windows PowerShell:**
```powershell
# Set IP LAN
$env:EUREKA_INSTANCE_IP="192.168.1.9"

# Restart tất cả services
# (Dừng và chạy lại trong IntelliJ hoặc terminal)
```

### Từ `192.168.1.9` → `127.0.0.1`:

**Windows PowerShell:**
```powershell
# Xóa biến môi trường (hoặc set về 127.0.0.1)
$env:EUREKA_INSTANCE_IP="127.0.0.1"
# Hoặc
Remove-Item Env:\EUREKA_INSTANCE_IP

# Restart tất cả services
```

---

## 💡 Các Kịch Bản Sử Dụng

### Kịch Bản 1: Development Local (1 máy)
```powershell
# Không set env var → Tự động dùng 127.0.0.1
cd auth-service
./mvnw spring-boot:run
```

### Kịch Bản 2: Test với Mobile App
```powershell
# Set IP LAN để mobile có thể truy cập
$env:EUREKA_INSTANCE_IP="192.168.1.9"
cd auth-service
./mvnw spring-boot:run
```

### Kịch Bản 3: Team Development
```powershell
# Set IP LAN để team có thể truy cập
$env:EUREKA_INSTANCE_IP="192.168.1.9"
cd auth-service
./mvnw spring-boot:run
```

---

## 🎯 Lưu Ý Quan Trọng

### ⚠️ Tất Cả Services Phải Dùng Cùng IP

Khi bạn set `EUREKA_INSTANCE_IP=192.168.1.9`, bạn **phải set cùng giá trị** cho:
- ✅ `auth-service`
- ✅ `ai-service`
- ✅ `gateway`
- ✅ Tất cả services khác

**Lý do:** Các service cần gọi nhau qua Eureka. Nếu IP không khớp, sẽ không tìm thấy nhau.

### ✅ Cách Set Cho Tất Cả Services

**Windows PowerShell:**
```powershell
# Set một lần cho tất cả
$env:EUREKA_INSTANCE_IP="192.168.1.9"
$env:EUREKA_INSTANCE_HOSTNAME="localhost"

# Sau đó chạy tất cả services trong cùng terminal session
# (Hoặc set trong mỗi terminal riêng)
```

**Hoặc trong IntelliJ IDEA:**
1. `Run` → `Edit Configurations...`
2. Chọn từng service (AuthServiceApplication, AiServiceApplication, GatewayApplication)
3. Trong `Environment variables`, thêm:
   - `EUREKA_INSTANCE_IP=192.168.1.9`
   - `EUREKA_INSTANCE_HOSTNAME=localhost`
4. Apply và chạy lại

---

## 🔍 Kiểm Tra IP Đã Được Sử Dụng

### Cách 1: Xem trong Eureka Dashboard
1. Mở: `http://localhost:8761`
2. Xem phần "Instances currently registered with Eureka"
3. Kiểm tra IP hiển thị:
   - `127.0.0.1:ai-service:9001` → Đang dùng localhost
   - `192.168.1.9:ai-service:9001` → Đang dùng LAN IP

### Cách 2: Xem trong Log
Tìm dòng log khi service khởi động:
```
DiscoveryClient_AI-SERVICE/127.0.0.1:ai-service:9001 - registration status: 204
```
→ `127.0.0.1` là IP đang được sử dụng

---

## 📝 Tóm Tắt

| Tình Huống | Cách Làm | IP Sử Dụng |
|------------|----------|------------|
| **Local dev (mặc định)** | Không làm gì | `127.0.0.1` |
| **Test với mobile/team** | Set `EUREKA_INSTANCE_IP=192.168.1.9` | `192.168.1.9` |
| **Quay lại local** | Xóa env var hoặc set `127.0.0.1` | `127.0.0.1` |

---

## 🎉 Lợi Ích

✅ **Không cần sửa code** - Chỉ cần set biến môi trường  
✅ **Chuyển đổi nhanh** - Dễ dàng switch giữa 2 IP  
✅ **Linh hoạt** - Dùng cho nhiều môi trường khác nhau  
✅ **An toàn** - Mặc định dùng localhost (an toàn hơn)  

---

## 🆘 Troubleshooting

### Vấn đề: Services không tìm thấy nhau

**Nguyên nhân:** Các service đang dùng IP khác nhau

**Giải pháp:**
1. Kiểm tra tất cả services đã set cùng `EUREKA_INSTANCE_IP` chưa
2. Restart tất cả services
3. Kiểm tra Eureka dashboard để xem IP của từng service

### Vấn đề: Vẫn dùng IP cũ sau khi set env var

**Nguyên nhân:** Service chưa được restart

**Giải pháp:**
1. Dừng service
2. Set lại biến môi trường
3. Restart service

---

**Chúc bạn sử dụng thành công! 🚀**

