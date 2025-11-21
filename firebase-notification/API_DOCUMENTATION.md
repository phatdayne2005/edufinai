## Firebase Notification Service API

Base URL: `/api/notifications` (đi qua Gateway hoặc base URL nội bộ tùy môi trường).  
Tất cả endpoint yêu cầu người dùng đã đăng nhập; frontend cần gửi kèm JWT token trong header `Authorization: Bearer <token>`.

**Lưu ý quan trọng**: Backend tự động lấy `userId` (UUID) từ auth-service dựa trên JWT token, frontend không cần gửi userId trong request body.

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
- **Path Parameter**: `userId` (UUID format)
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
- **Response**: `202 Accepted`.
- **Ghi chú**: 
  - Endpoint này dành cho **service nội bộ** hoặc **admin UI**.
  - Frontend thường **không gọi trực tiếp** để tránh giả mạo userId.
  - Gửi thông báo tới tất cả token FCM đang active của user đó.

**Ví dụ từ service khác**:
```java
fcmService.sendToUser(userId, "Thông báo", "Bạn có tin nhắn mới", Map.of("type", "message"));
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

### Gửi thông báo tới user
**Request** (từ service khác):
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
