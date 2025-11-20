# Hướng Dẫn Cấu Hình Eureka Instance IP

## 📋 Tổng Quan

Tất cả các service (ai-service, auth-service, gateway) đã được cấu hình để sử dụng **biến môi trường** cho IP address. Điều này giúp bạn:
- ✅ **Local development**: Không cần làm gì, tự động dùng `127.0.0.1`
- ✅ **Staging/Production**: Chỉ cần set biến môi trường khi deploy
- ✅ **Không cần sửa code**: Một lần cấu hình, dùng cho mọi môi trường

---

## 🏠 Môi Trường Local (Development)

### Cách 1: Chạy bình thường (Khuyến nghị)
**Không cần làm gì cả!** Các service sẽ tự động dùng `127.0.0.1` (localhost).

```bash
# Chạy service như bình thường
cd ai-service
./mvnw spring-boot:run

# Hoặc trong IDE, chạy như bình thường
```

### Cách 2: Chạy với IP cụ thể (nếu cần test trên LAN)
Nếu bạn muốn các service có thể truy cập từ máy khác trong mạng LAN:

**Windows PowerShell:**
```powershell
$env:EUREKA_INSTANCE_IP="192.168.1.9"  # IP của máy bạn (xem bằng ipconfig)
cd ai-service
./mvnw spring-boot:run
```

**Windows CMD:**
```cmd
set EUREKA_INSTANCE_IP=192.168.1.9
cd ai-service
mvnw spring-boot:run
```

**Linux/macOS:**
```bash
export EUREKA_INSTANCE_IP=192.168.1.9
cd ai-service
./mvnw spring-boot:run
```

---

## 🚀 Môi Trường Staging/Production

### Cách 1: Set biến môi trường trước khi chạy

**Windows PowerShell:**
```powershell
$env:EUREKA_INSTANCE_IP="192.168.1.100"  # IP của server
$env:EUREKA_INSTANCE_HOSTNAME="server-prod"
./mvnw spring-boot:run
```

**Linux/macOS:**
```bash
export EUREKA_INSTANCE_IP=192.168.1.100
export EUREKA_INSTANCE_HOSTNAME=server-prod
./mvnw spring-boot:run
```

### Cách 2: Dùng file `.env` (nếu dùng Docker Compose)

Tạo file `.env`:
```env
EUREKA_INSTANCE_IP=192.168.1.100
EUREKA_INSTANCE_HOSTNAME=server-prod
```

Trong `docker-compose.yml`:
```yaml
services:
  ai-service:
    image: ai-service:latest
    environment:
      - EUREKA_INSTANCE_IP=${EUREKA_INSTANCE_IP}
      - EUREKA_INSTANCE_HOSTNAME=${EUREKA_INSTANCE_HOSTNAME}
    env_file:
      - .env
```

### Cách 3: Dùng Kubernetes ConfigMap/Secret

**Tạo ConfigMap:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: eureka-config
data:
  EUREKA_INSTANCE_IP: "192.168.1.100"
  EUREKA_INSTANCE_HOSTNAME: "server-prod"
```

**Trong Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-service
spec:
  template:
    spec:
      containers:
      - name: ai-service
        image: ai-service:latest
        envFrom:
        - configMapRef:
            name: eureka-config
```

### Cách 4: Dùng Spring Profile (nếu muốn config trong file)

Tạo file `application-prod.yaml`:
```yaml
eureka:
  instance:
    ip-address: 192.168.1.100
    hostname: server-prod
```

Chạy với profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 🔍 Kiểm Tra IP Đã Được Sử Dụng

### Cách 1: Xem trong Eureka UI
1. Mở trình duyệt: `http://localhost:8761`
2. Tìm service của bạn (ví dụ: `AI-SERVICE`)
3. Xem cột **Status** → IP hiển thị ở đó là IP đã được đăng ký

### Cách 2: Xem trong Log
Khi service khởi động, tìm dòng log:
```
DiscoveryClient_AI-SERVICE/127.0.0.1:ai-service:9001 - registration status: 204
```
→ `127.0.0.1` là IP đã được sử dụng

### Cách 3: Kiểm tra bằng lệnh (Windows)
```powershell
# Xem IP của máy
ipconfig

# Xem IP nào đang được dùng cho Eureka
# (Kiểm tra trong Eureka UI hoặc log)
```

---

## ⚠️ Lưu Ý Quan Trọng

### 1. Tất cả service phải dùng cùng IP
Nếu bạn set `EUREKA_INSTANCE_IP=192.168.1.9` cho `ai-service`, bạn cũng phải set **cùng giá trị** cho:
- `auth-service`
- `gateway`
- Các service khác trong cùng môi trường

**Lý do:** Các service cần gọi nhau qua Eureka. Nếu IP không khớp, sẽ không tìm thấy nhau.

### 2. IP phải có thể truy cập được
- **Local:** Dùng `127.0.0.1` hoặc `localhost`
- **LAN:** Dùng IP nội bộ (ví dụ: `192.168.1.9`)
- **Internet:** Dùng IP public hoặc domain name

### 3. Firewall
Đảm bảo firewall cho phép kết nối đến port của service:
- `ai-service`: Port `9001`
- `auth-service`: Port `9000`
- `gateway`: Port `8080`
- `eureka`: Port `8761`

---

## 📝 Tóm Tắt Nhanh

| Môi Trường | Cách Làm | Ví Dụ |
|------------|----------|-------|
| **Local** | Không làm gì | Tự động dùng `127.0.0.1` |
| **LAN** | Set env var | `$env:EUREKA_INSTANCE_IP="192.168.1.9"` |
| **Production** | Set env var khi deploy | `export EUREKA_INSTANCE_IP="192.168.1.100"` |
| **Docker** | Dùng `.env` file | Xem Cách 2 ở trên |
| **Kubernetes** | Dùng ConfigMap | Xem Cách 3 ở trên |

---

## 🆘 Xử Lý Lỗi

### Lỗi: "Unable to find instance for SERVICE"
**Nguyên nhân:** Service chưa đăng ký vào Eureka hoặc IP không đúng.

**Giải pháp:**
1. Kiểm tra service đã chạy chưa
2. Kiểm tra Eureka UI: `http://localhost:8761`
3. Kiểm tra IP trong log của service
4. Đảm bảo tất cả service dùng cùng IP

### Lỗi: "Failed to resolve hostname"
**Nguyên nhân:** Eureka trả về hostname không resolve được.

**Giải pháp:**
1. Set `prefer-ip-address: true` (đã có sẵn)
2. Set `ip-address` bằng biến môi trường (đã cấu hình)
3. Kiểm tra IP đã đúng chưa

---

## ✅ Checklist Khi Deploy

- [ ] Đã set `EUREKA_INSTANCE_IP` cho tất cả service
- [ ] Tất cả service dùng cùng IP
- [ ] Firewall đã mở port
- [ ] Kiểm tra Eureka UI thấy tất cả service đã đăng ký
- [ ] Test gọi API qua Gateway thành công

---

**Chúc bạn thành công! 🎉**

