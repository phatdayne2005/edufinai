## Firebase Notification Service API

**Base URL**: 
- **Gateway**: `http://localhost:8080/notification` (cho frontend và service qua Gateway)
- **Direct Service**: `http://firebase-notification-service:8080/api/notifications` (cho service-to-service)

### Authentication Requirements

| Endpoint | Authentication | Mục đích |
|----------|----------------|----------|
| `POST /register-token` | ✅ JWT Required | Frontend đăng ký FCM token |
| `DELETE /token` | ✅ JWT Required | Frontend hủy FCM token |
| `POST /user/{userId}` | ❌ **NO JWT** | Service-to-service gửi thông báo |
| `POST /topic/{topic}` | ✅ JWT Required | Gửi thông báo tới topic |
| `POST /broadcast` | ✅ JWT Required | Broadcast toàn hệ thống |

**Lưu ý quan trọng**: 
- Các endpoint yêu cầu JWT: Backend tự động lấy `userId` (UUID) từ auth-service dựa trên JWT token, frontend **không cần** gửi userId trong request body.
- Endpoint `/user/{userId}` **không yêu cầu JWT** để các service khác (Learning, Finance, Gamification, AI) có thể gọi trực tiếp mà không cần authenticate.

---

### 1. Đăng ký token thiết bị

- **Method**: `POST /register-token`
- **Headers**: 
  ```
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json
  ```
- **Body**:
  ```json
  {
    "token": "string",          // FCM registration token từ Firebase
    "platform": "web",          // "web", "android", "ios", ...
    "deviceInfo": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) ..."  // User agent hoặc mô tả thiết bị (TEXT)
  }
  ```
- **Mục đích**: Lưu token FCM của user hiện tại (tự động lấy userId từ JWT) để backend có thể gửi thông báo.
- **Response**: `200 OK` (không payload).

**Ví dụ frontend React**:
```typescript
import { getMessaging, getToken } from "firebase/messaging";

const messaging = getMessaging(app);
const token = await getToken(messaging, {
  vapidKey: process.env.REACT_APP_VAPID_KEY
});

// Gửi token lên backend (JWT token tự động được gửi trong header)
await api.post("/api/notifications/register-token", {
  token,
  platform: "web",
  deviceInfo: navigator.userAgent  // Có thể gửi chuỗi trực tiếp, không cần JSON
}, {
  headers: {
    Authorization: `Bearer ${localStorage.getItem('jwt_token')}`
  }
});
```

---

### 2. Hủy token thiết bị

- **Method**: `DELETE /token`
- **Headers**: 
  ```
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json
  ```
- **Body**:
  ```json
  {
    "token": "string"  // FCM token cần hủy
  }
  ```
- **Mục đích**: Vô hiệu token khi người dùng logout hoặc muốn tắt thông báo. Backend tự động xác định userId từ JWT.
- **Response**: `200 OK`.

**Ví dụ frontend**:
```typescript
await api.delete("/api/notifications/token", {
  data: { token: fcmToken },
  headers: {
    Authorization: `Bearer ${localStorage.getItem('jwt_token')}`
  }
});
```

---

### 3. Gửi thông báo tới 1 user (Service/Admin only)

- **Method**: `POST /user/{userId}`  
- **Gateway URL**: `POST /notification/user/{userId}` (qua Gateway)  
- **Direct URL**: `POST http://firebase-notification-service:8080/api/notifications/user/{userId}` (service-to-service)
- **Path Parameter**: `userId` (UUID format, ví dụ: `550e8400-e29b-41d4-a716-446655440000`)
- **Authentication**: ❌ **KHÔNG CẦN JWT** (endpoint này được permit trong SecurityConfig để các service khác có thể gọi trực tiếp)
- **Headers**: 
  ```
  Content-Type: application/json
  ```
- **Body**:
  ```json
  {
    "title": "Tiêu đề thông báo",
    "body": "Nội dung thông báo",
    "data": {
      "key1": "value1",
      "key2": "value2"
    }
  }
  ```
- **Response**: `202 Accepted` (không có response body)
- **Ghi chú**: 
  - Endpoint này dành cho **service nội bộ** (AI Service, Learning Service, Finance Service, Gamification Service) hoặc **admin UI**.
  - **KHÔNG yêu cầu JWT token** để các service có thể gọi trực tiếp mà không cần authenticate.
  - Frontend thường **không nên gọi** endpoint này để tránh giả mạo userId (frontend nên dùng các endpoint khác yêu cầu JWT).
  - Gửi thông báo tới **tất cả token FCM đang active** của user đó (nếu user có nhiều thiết bị, tất cả đều nhận được).

**Ví dụ từ service khác (Java/RestTemplate)**:
```java
// Trong Learning Service, Finance Service, hoặc Gamification Service
@Autowired
private RestTemplate restTemplate;

public void sendNotificationToUser(UUID userId, String title, String body) {
    String url = "http://firebase-notification-service/api/notifications/user/" + userId;
    
    Map<String, Object> payload = new HashMap<>();
    payload.put("title", title);
    payload.put("body", body);
    payload.put("data", Map.of("type", "achievement", "source", "gamification"));
    
    restTemplate.postForEntity(url, payload, Void.class);
}
```

**Ví dụ từ service khác (Feign Client)**:
```java
@FeignClient(name = "firebase-notification-service", path = "/api/notifications")
public interface NotificationServiceClient {
    
    @PostMapping("/user/{userId}")
    void sendToUser(
        @PathVariable("userId") UUID userId,
        @RequestBody NotificationRequest request
    );
}

// Sử dụng
notificationServiceClient.sendToUser(
    userId,
    new NotificationRequest("Thông báo", "Bạn đã hoàn thành challenge!", 
        Map.of("type", "challenge", "challengeId", challengeId))
);
```

**Ví dụ cURL (từ service hoặc admin)**:
```bash
curl -X POST http://localhost:8080/notification/user/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Thông báo mới",
    "body": "Bạn có tin nhắn mới từ hệ thống",
    "data": {
      "type": "message",
      "messageId": "123"
    }
  }'
```

---

### 4. Gửi thông báo tới topic

- **Method**: `POST /topic/{topic}`
- **Path Parameter**: `topic` (string, ví dụ: "all", "premium", "news")
- **Body**: Giống endpoint `/user/{userId}`
- **Response**: `202 Accepted`.
- **Ghi chú**: 
  - Người dùng phải được subscribe vào topic trước.
  - Backend tự động subscribe token vào topic mặc định (`all`) sau khi đăng ký token.

---

### 5. Broadcast toàn hệ thống

- **Method**: `POST /broadcast`
- **Body**: Giống endpoint `/user/{userId}`
- **Response**: `202 Accepted`.
- **Ghi chú**: 
  - Hiện tại gửi tới topic mặc định (`fcm.default-topic`, giá trị `all`).
  - Tất cả user đã đăng ký token sẽ nhận được thông báo.

---

## Integration Guide cho Backend Services

### Cách các Service khác gọi Firebase Notification Service

Các service như **Learning Service**, **Finance Service**, **Gamification Service**, **AI Service** có thể gọi endpoint `/user/{userId}` để gửi thông báo cho user mà **không cần JWT token**.

#### Option 1: Sử dụng RestTemplate (Spring Boot)

```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
public class NotificationService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${services.notification.base-url:http://firebase-notification-service}")
    private String notificationServiceUrl;
    
    public void sendNotification(UUID userId, String title, String body, Map<String, Object> data) {
        String url = notificationServiceUrl + "/api/notifications/user/" + userId;
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("body", body);
        payload.put("data", data != null ? data : new HashMap<>());
        
        try {
            restTemplate.postForEntity(url, payload, Void.class);
        } catch (Exception e) {
            log.error("Failed to send notification to user {}", userId, e);
            // Không throw exception để không ảnh hưởng flow chính
        }
    }
}
```

#### Option 2: Sử dụng Feign Client (Spring Cloud OpenFeign)

**Thêm dependency**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

**Tạo Feign Client**:
```java
@FeignClient(name = "firebase-notification-service", path = "/api/notifications")
public interface NotificationServiceClient {
    
    @PostMapping("/user/{userId}")
    void sendToUser(
        @PathVariable("userId") UUID userId,
        @RequestBody NotificationRequest request
    );
}

@Data
public class NotificationRequest {
    private String title;
    private String body;
    private Map<String, Object> data;
}
```

**Sử dụng**:
```java
@Service
@RequiredArgsConstructor
public class GoalService {
    
    private final NotificationServiceClient notificationClient;
    
    public void completeGoal(UUID userId, UUID goalId) {
        // ... xử lý logic ...
        
        // Gửi thông báo
        NotificationRequest request = new NotificationRequest();
        request.setTitle("Chúc mừng!");
        request.setBody("Bạn đã hoàn thành mục tiêu tài chính");
        request.setData(Map.of(
            "type", "goal_completed",
            "goalId", goalId.toString()
        ));
        
        try {
            notificationClient.sendToUser(userId, request);
        } catch (Exception e) {
            log.error("Failed to send notification", e);
        }
    }
}
```

#### Option 3: Sử dụng Eureka Service Discovery

Nếu các service đều đăng ký với Eureka, có thể dùng service name:

```java
// application.properties
services.notification.service-name=firebase-notification-service

// Code
@Value("${services.notification.service-name}")
private String serviceName;

String url = "http://" + serviceName + "/api/notifications/user/" + userId;
```

**Lưu ý**:
- Endpoint `/user/{userId}` **không yêu cầu JWT**, nên có thể gọi trực tiếp
- Nên wrap trong try-catch để không ảnh hưởng flow chính nếu notification service lỗi
- Có thể dùng `@Async` để gửi notification bất đồng bộ

---

## Checklist tích hợp frontend React

### Bước 1: Cài đặt Firebase SDK
```bash
npm install firebase
```

### Bước 2: Cấu hình Firebase
Tạo file `src/firebase/config.js`:
```javascript
import { initializeApp } from 'firebase/app';
import { getMessaging } from 'firebase/messaging';

const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_PROJECT.firebaseapp.com",
  projectId: "YOUR_PROJECT_ID",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
};

const app = initializeApp(firebaseConfig);
export const messaging = getMessaging(app);
```

### Bước 3: Tạo Service Worker
Tạo file `public/firebase-messaging-sw.js`:
```javascript
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_PROJECT.firebaseapp.com",
  projectId: "YOUR_PROJECT_ID",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: '/logo192.png'
  };
  self.registration.showNotification(notificationTitle, notificationOptions);
});
```

### Bước 4: Xin quyền và đăng ký token
```typescript
import { getToken, onMessage } from 'firebase/messaging';
import { messaging } from './firebase/config';

// Xin quyền thông báo
const requestPermission = async () => {
  const permission = await Notification.requestPermission();
  if (permission === 'granted') {
    try {
      const token = await getToken(messaging, {
        vapidKey: process.env.REACT_APP_VAPID_KEY
      });
      
      if (token) {
        // Gửi token lên backend
        await registerToken(token);
      }
    } catch (error) {
      console.error('Error getting token:', error);
    }
  }
};

// Đăng ký token với backend
const registerToken = async (fcmToken: string) => {
  await api.post('/api/notifications/register-token', {
    token: fcmToken,
    platform: 'web',
    deviceInfo: navigator.userAgent
  }, {
    headers: {
      Authorization: `Bearer ${localStorage.getItem('jwt_token')}`
    }
  });
};

// Lắng nghe thông báo khi app đang mở
onMessage(messaging, (payload) => {
  console.log('Message received:', payload);
  // Hiển thị thông báo trong app
});
```

### Bước 5: Xử lý logout
```typescript
const handleLogout = async () => {
  const fcmToken = localStorage.getItem('fcm_token');
  if (fcmToken) {
    await api.delete('/api/notifications/token', {
      data: { token: fcmToken },
      headers: {
        Authorization: `Bearer ${localStorage.getItem('jwt_token')}`
      }
    });
  }
  // ... xóa JWT token và logout
};
```

---

## Cơ chế bảo mật & mapping

### Authentication Flow
1. Frontend gửi request với JWT token trong header `Authorization: Bearer <token>`.
2. Backend (notification-service) nhận JWT và forward sang auth-service để lấy thông tin user.
3. Auth-service trả về `UserInfo` chứa `userId` (UUID).
4. Backend sử dụng `userId` này để lưu/query FCM token.

### Firebase Project Mapping
- **Frontend**: Nhận token FCM từ Firebase project (ví dụ `edufinai`).
- **Backend**: Dùng service account JSON của cùng project để gửi thông báo.
- Firebase tự động kiểm tra token và service account có cùng project → chấp nhận gửi.
- **Yêu cầu**: Đảm bảo frontend dùng đúng Firebase config và backend cấu hình `fcm.service-account-file` trỏ đúng file JSON.

### Database Schema
- `user_id`: UUID (không phải Long)
- `device_info`: TEXT (có thể lưu user-agent hoặc mô tả thiết bị dạng chuỗi)
- `token`: VARCHAR(512), unique
- `platform`: VARCHAR (web, android, ios, ...)
- `is_active`: BOOLEAN
- `created_at`, `last_seen`: TIMESTAMP

---

## Error Handling

### 401 Unauthorized
- JWT token không hợp lệ hoặc đã hết hạn.
- Giải pháp: Yêu cầu user đăng nhập lại.

### 400 Bad Request
- Request body không đúng format.
- `deviceInfo` có thể là chuỗi TEXT bất kỳ (không cần JSON).

### 500 Internal Server Error
- Lỗi khi gọi auth-service hoặc Firebase.
- Kiểm tra log backend để xem chi tiết.

---

## Ví dụ request/response

### Đăng ký token thành công
**Request**:
```http
POST /api/notifications/register-token
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json

{
  "token": "fkb7uUzTGLH6jArD6iaJ0M:APA91bEn9nxGH97hIkaRvRGsX0cw_Xt_v3laCqcIZUjscVVJ0xVBK_NNy_SY_fxi547Ftyn2eHJjFZQhufH2kls2th6aGHnz28MxweDFjPbOclIv-ZHcgxU",
  "platform": "web",
  "deviceInfo": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
}
```

**Response**:
```http
HTTP/1.1 200 OK
```

### Gửi thông báo tới user (Service-to-Service)
**Request** (từ service khác, **KHÔNG CẦN JWT**):
```http
POST /api/notifications/user/550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "title": "Thông báo mới",
  "body": "Bạn có tin nhắn mới từ hệ thống",
  "data": {
    "type": "message",
    "messageId": "123"
  }
}
```

**Response**:
```http
HTTP/1.1 202 Accepted
```

**Lưu ý**: 
- Request này **KHÔNG cần** header `Authorization: Bearer <JWT_TOKEN>`
- Endpoint này được permit trong SecurityConfig để các service có thể gọi trực tiếp
- Nếu gọi qua Gateway: `POST /notification/user/{userId}` (Gateway sẽ forward tới service)
