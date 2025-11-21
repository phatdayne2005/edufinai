# Kế Hoạch Tích Hợp Firebase Cloud Messaging (FCM) - Lý Thuyết

## 📋 Tổng Quan

Tài liệu này mô tả chi tiết cách tích hợp FCM để gửi thông báo real-time cho client (ReactJS) khi có sự kiện gamification (ví dụ: cộng điểm, hoàn thành challenge).

---

## 🏗️ Kiến Trúc Tổng Thể

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   ReactJS   │────────▶│ Auth Service │────────▶│ Gamification│
│   Client    │         │              │         │   Service   │
└─────────────┘         └──────────────┘         └─────────────┘
      │                                              │
      │ 1. Register FCM Token                       │
      │────────────────────────────────────────────▶│
      │                                              │
      │ 2. Store Token in DB                         │
      │                                              │
      │                                              │ 3. Event: Add Reward
      │                                              │─────────────────┐
      │                                              │                 │
      │                                              │                 ▼
      │                                              │         ┌──────────────┐
      │                                              │         │ FCM Service  │
      │                                              │         │  (Backend)   │
      │                                              │         └──────────────┘
      │                                              │                 │
      │                                              │                 │ 4. Send Notification
      │                                              │                 │
      │                                              │                 ▼
      │                                              │         ┌──────────────┐
      │                                              │         │ Firebase FCM │
      │                                              │         │   Server     │
      │                                              │         └──────────────┘
      │                                              │                 │
      │                                              │                 │ 5. Push to Device
      │                                              │                 │
      │◀────────────────────────────────────────────┼─────────────────┘
      │ 6. Receive Notification                      │
      │                                              │
```

---

## 🔄 Flow Chi Tiết

### Phase 1: Setup & Registration (Lần đầu tiên)

#### Bước 1: Frontend - Khởi tạo Firebase trong ReactJS

**1.1. Cài đặt dependencies:**
```bash
npm install firebase
```

**1.2. Tạo Firebase config file:**
```javascript
// src/config/firebase.js
import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage } from 'firebase/messaging';

const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_AUTH_DOMAIN",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_STORAGE_BUCKET",
  messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
  appId: "YOUR_APP_ID"
};

const app = initializeApp(firebaseConfig);
export const messaging = getMessaging(app);

// Request permission và lấy FCM token
export const requestPermission = async () => {
  try {
    const permission = await Notification.requestPermission();
    if (permission === 'granted') {
      const token = await getToken(messaging, {
        vapidKey: 'YOUR_VAPID_KEY' // Lấy từ Firebase Console
      });
      return token;
    }
    return null;
  } catch (error) {
    console.error('Error getting FCM token:', error);
    return null;
  }
};

// Listen for foreground messages
export const onMessageListener = () => {
  return new Promise((resolve) => {
    onMessage(messaging, (payload) => {
      resolve(payload);
    });
  });
};
```

**1.3. Service Worker cho background messages:**
```javascript
// public/firebase-messaging-sw.js
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_AUTH_DOMAIN",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_STORAGE_BUCKET",
  messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
  appId: "YOUR_APP_ID"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  console.log('Background message received:', payload);
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: '/icon-192x192.png' // Icon của app
  };
  self.registration.showNotification(notificationTitle, notificationOptions);
});
```

#### Bước 2: Frontend - Lưu FCM Token sau khi Login

**2.1. Flow sau khi user login thành công:**
```javascript
// src/hooks/useFCM.js
import { useEffect, useState } from 'react';
import { requestPermission, onMessageListener } from '../config/firebase';
import { saveFCMToken } from '../services/gamificationService';

export const useFCM = (userId, isAuthenticated) => {
  const [token, setToken] = useState(null);

  useEffect(() => {
    if (!isAuthenticated || !userId) return;

    const setupFCM = async () => {
      try {
        // 1. Request permission
        const fcmToken = await requestPermission();
        
        if (fcmToken) {
          setToken(fcmToken);
          
          // 2. Gửi token lên backend để lưu
          await saveFCMToken(userId, fcmToken);
        }
      } catch (error) {
        console.error('FCM setup error:', error);
      }
    };

    setupFCM();

    // 3. Listen for foreground messages
    onMessageListener()
      .then((payload) => {
        console.log('Foreground message:', payload);
        // Hiển thị notification hoặc update UI
        showNotification(payload);
      })
      .catch((err) => console.error('Message listener error:', err));

  }, [userId, isAuthenticated]);

  return token;
};

// Component sử dụng
const App = () => {
  const { user, isAuthenticated } = useAuth();
  useFCM(user?.id, isAuthenticated);

  return <div>...</div>;
};
```

**2.2. API call để lưu token:**
```javascript
// src/services/gamificationService.js
export const saveFCMToken = async (userId, fcmToken) => {
  const response = await fetch('/api/v1/gamify/fcm/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken()}`
    },
    body: JSON.stringify({
      userId: userId,
      fcmToken: fcmToken,
      deviceType: 'WEB' // hoặc 'MOBILE'
    })
  });
  return response.json();
};
```

#### Bước 3: Backend - API để lưu FCM Token

**3.1. Database Schema:**
```sql
CREATE TABLE user_fcm_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(36) NOT NULL,
    fcm_token VARCHAR(255) NOT NULL,
    device_type VARCHAR(20) DEFAULT 'WEB',
    device_info TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    UNIQUE KEY unique_user_token (user_id, fcm_token),
    INDEX idx_user_id (user_id),
    INDEX idx_fcm_token (fcm_token)
);
```

**3.2. Model:**
```java
// UserFCMToken.java
@Entity
@Table(name = "user_fcm_tokens")
public class UserFCMToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @Column(name = "fcm_token", nullable = false, length = 255)
    private String fcmToken;
    
    @Column(name = "device_type", length = 20)
    private String deviceType;
    
    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Getters, setters, constructors
}
```

**3.3. DTO:**
```java
// FCMTokenRequest.java
public class FCMTokenRequest {
    @NotBlank(message = "User ID không được để trống")
    private String userId;
    
    @NotBlank(message = "FCM Token không được để trống")
    private String fcmToken;
    
    private String deviceType; // WEB, MOBILE
    
    private String deviceInfo; // Browser info, device model, etc.
    
    // Getters, setters
}

// FCMTokenResponse.java
public class FCMTokenResponse {
    private Long id;
    private String userId;
    private String status; // SUCCESS, FAILED
    private String message;
    
    // Getters, setters
}
```

**3.4. Repository:**
```java
@Repository
public interface UserFCMTokenRepository extends JpaRepository<UserFCMToken, Long> {
    List<UserFCMToken> findByUserIdAndIsActiveTrue(String userId);
    Optional<UserFCMToken> findByUserIdAndFcmToken(String userId, String fcmToken);
    void deleteByFcmToken(String fcmToken);
    void deleteByUserId(String userId);
}
```

**3.5. Service:**
```java
@Service
public class FCMTokenService {
    private final UserFCMTokenRepository tokenRepository;
    
    public FCMTokenResponse saveToken(FCMTokenRequest request) {
        // 1. Kiểm tra token đã tồn tại chưa
        Optional<UserFCMToken> existing = tokenRepository
            .findByUserIdAndFcmToken(request.getUserId(), request.getFcmToken());
        
        if (existing.isPresent()) {
            // Update existing token
            UserFCMToken token = existing.get();
            token.setIsActive(true);
            token.setUpdatedAt(LocalDateTime.now());
            tokenRepository.save(token);
        } else {
            // Deactivate old tokens của user này (optional - có thể giữ nhiều devices)
            // tokenRepository.findByUserIdAndIsActiveTrue(request.getUserId())
            //     .forEach(t -> t.setIsActive(false));
            
            // Tạo token mới
            UserFCMToken newToken = new UserFCMToken();
            newToken.setUserId(request.getUserId());
            newToken.setFcmToken(request.getFcmToken());
            newToken.setDeviceType(request.getDeviceType());
            newToken.setDeviceInfo(request.getDeviceInfo());
            newToken.setCreatedAt(LocalDateTime.now());
            tokenRepository.save(newToken);
        }
        
        return new FCMTokenResponse(/* ... */);
    }
    
    public List<String> getActiveTokensByUserId(String userId) {
        return tokenRepository.findByUserIdAndIsActiveTrue(userId)
            .stream()
            .map(UserFCMToken::getFcmToken)
            .collect(Collectors.toList());
    }
}
```

**3.6. Controller:**
```java
@RestController
@RequestMapping("/api/v1/gamify/fcm")
@Tag(name = "FCM Token Controller")
public class FCMTokenController {
    private final FCMTokenService fcmTokenService;
    
    @PostMapping("/token")
    @Operation(summary = "Save FCM token for user")
    public ResponseEntity<FCMTokenResponse> saveToken(
            @Valid @RequestBody FCMTokenRequest request) {
        FCMTokenResponse response = fcmTokenService.saveToken(request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/token")
    @Operation(summary = "Remove FCM token (on logout)")
    public ResponseEntity<Void> removeToken(@RequestParam String fcmToken) {
        fcmTokenService.removeToken(fcmToken);
        return ResponseEntity.ok().build();
    }
}
```

---

### Phase 2: Gửi Notification khi có Event

#### Bước 4: Backend - Setup Firebase Admin SDK

**4.1. Thêm dependency vào pom.xml:**
```xml
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>
```

**4.2. Download service account key từ Firebase Console:**
- Vào Firebase Console → Project Settings → Service Accounts
- Generate new private key → Download JSON file
- Lưu file này vào `src/main/resources/firebase-service-account.json` (hoặc dùng environment variable)

**4.3. Firebase Config:**
```java
@Configuration
public class FirebaseConfig {
    
    @Value("${firebase.service-account.path:classpath:firebase-service-account.json}")
    private String serviceAccountPath;
    
    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount = getClass().getClassLoader()
                .getResourceAsStream("firebase-service-account.json");
            
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
            
            FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }
}
```

**4.4. FCM Service:**
```java
@Service
@Slf4j
public class FCMNotificationService {
    private final FCMTokenService fcmTokenService;
    
    public void sendRewardNotification(String userId, Reward reward) {
        // 1. Lấy danh sách FCM tokens của user
        List<String> tokens = fcmTokenService.getActiveTokensByUserId(userId);
        
        if (tokens.isEmpty()) {
            log.warn("No FCM tokens found for user: {}", userId);
            return;
        }
        
        // 2. Tạo notification message
        Notification notification = Notification.builder()
            .setTitle("🎉 Bạn đã nhận được điểm thưởng!")
            .setBody(String.format("Bạn nhận được %d điểm cho badge: %s", 
                reward.getScore(), reward.getBadge()))
            .build();
        
        // 3. Tạo data payload (optional - để handle khi app đang mở)
        Map<String, String> data = new HashMap<>();
        data.put("type", "REWARD");
        data.put("rewardId", reward.getRewardId().toString());
        data.put("score", String.valueOf(reward.getScore()));
        data.put("badge", reward.getBadge());
        data.put("reason", reward.getReason() != null ? reward.getReason() : "");
        
        // 4. Tạo message
        MulticastMessage message = MulticastMessage.builder()
            .setNotification(notification)
            .putAllData(data)
            .addAllTokens(tokens)
            .setAndroidConfig(AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build())
            .setApnsConfig(ApnsConfig.builder()
                .setAps(Aps.builder()
                    .setSound("default")
                    .setBadge(1)
                    .build())
                .build())
            .build();
        
        // 5. Gửi notification
        try {
            BatchResponse response = FirebaseMessaging.getInstance()
                .sendMulticast(message);
            
            log.info("Successfully sent {} messages, {} failed", 
                response.getSuccessCount(), response.getFailureCount());
            
            // 6. Xử lý failed tokens (có thể invalid, cần xóa)
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        String token = tokens.get(i);
                        log.warn("Failed to send to token {}: {}", 
                            token, responses.get(i).getException().getMessage());
                        
                        // Nếu token invalid, xóa khỏi DB
                        if (responses.get(i).getException() instanceof 
                            FirebaseMessagingException) {
                            FirebaseMessagingException e = 
                                (FirebaseMessagingException) responses.get(i).getException();
                            if (e.getErrorCode().equals("invalid-argument") || 
                                e.getErrorCode().equals("registration-token-not-registered")) {
                                fcmTokenService.removeToken(token);
                            }
                        }
                    }
                }
            }
        } catch (FirebaseMessagingException e) {
            log.error("Error sending FCM notification", e);
        }
    }
    
    // Method cho các loại notification khác
    public void sendChallengeCompletedNotification(String userId, Challenge challenge) {
        // Similar implementation
    }
    
    public void sendLeaderboardUpdateNotification(String userId, int newRank) {
        // Similar implementation
    }
}
```

#### Bước 5: Tích hợp vào RewardService

**5.1. Update RewardService:**
```java
@Service
public class RewardService {
    private final RewardRepository rewardRepository;
    private final FCMNotificationService fcmNotificationService; // Thêm dependency
    // ... other dependencies
    
    @Transactional
    public RewardResponse addReward(RewardRequest req) {
        // ... existing logic ...
        
        Reward reward = new Reward();
        reward.setUserId(req.getUserId());
        reward.setBadge(req.getBadge());
        reward.setScore(req.getScore());
        reward.setReason(req.getReason());
        reward.setCreatedAt(LocalDateTime.now());
        
        this.rewardRepository.save(reward);
        this.userRewardSummaryService.addSumaryReward(reward.getUserId(), reward.getScore());
        
        // Update leaderboards...
        
        // ✅ Gửi notification (async để không block response)
        try {
            fcmNotificationService.sendRewardNotification(
                req.getUserId().toString(), 
                reward
            );
        } catch (Exception e) {
            log.error("Failed to send FCM notification", e);
            // Không throw exception để không ảnh hưởng đến flow chính
        }
        
        return new RewardResponse(reward.getRewardId(), "SUCCESS");
    }
}
```

**5.2. Hoặc sử dụng @Async để không block:**
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("fcm-async-");
        executor.initialize();
        return executor;
    }
}

// Trong FCMNotificationService
@Async
public void sendRewardNotification(String userId, Reward reward) {
    // ... implementation
}
```

---

### Phase 3: Frontend - Xử lý Notification

#### Bước 6: ReactJS - Handle Notification

**6.1. Component để hiển thị notification:**
```javascript
// src/components/NotificationHandler.jsx
import { useEffect } from 'react';
import { onMessageListener } from '../config/firebase';
import { useNotification } from '../context/NotificationContext';

const NotificationHandler = () => {
  const { showNotification } = useNotification();

  useEffect(() => {
    onMessageListener()
      .then((payload) => {
        console.log('Message received:', payload);
        
        // Hiển thị notification
        if (payload.notification) {
          showNotification({
            title: payload.notification.title,
            body: payload.notification.body,
            type: payload.data?.type || 'INFO'
          });
        }
        
        // Update UI dựa trên data payload
        if (payload.data) {
          handleNotificationData(payload.data);
        }
      })
      .catch((err) => console.error('Error listening to messages:', err));
  }, []);

  const handleNotificationData = (data) => {
    switch (data.type) {
      case 'REWARD':
        // Update reward state, show toast, etc.
        updateUserRewards();
        break;
      case 'CHALLENGE_COMPLETED':
        // Update challenge state
        refreshChallenges();
        break;
      default:
        break;
    }
  };

  return null; // Component này không render gì
};
```

**6.2. Notification Context:**
```javascript
// src/context/NotificationContext.jsx
import { createContext, useContext, useState } from 'react';
import { toast } from 'react-toastify'; // hoặc custom notification component

const NotificationContext = createContext();

export const NotificationProvider = ({ children }) => {
  const showNotification = ({ title, body, type = 'INFO' }) => {
    toast.info(`${title}: ${body}`, {
      position: 'top-right',
      autoClose: 5000,
    });
  };

  return (
    <NotificationContext.Provider value={{ showNotification }}>
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotification = () => useContext(NotificationContext);
```

---

## 🔐 Security Considerations

### 1. Token Validation
- Validate FCM token format trước khi lưu
- Kiểm tra token có hợp lệ với Firebase không (optional)

### 2. User Authorization
- Chỉ cho phép user lưu token của chính mình
- Validate userId từ JWT token, không trust từ request body

### 3. Rate Limiting
- Giới hạn số lượng notifications gửi cho một user trong khoảng thời gian
- Tránh spam notifications

### 4. Token Cleanup
- Tự động xóa invalid tokens
- Xóa tokens cũ khi user logout
- Xóa tokens không được sử dụng trong X ngày

---

## 📊 Database Design

### Tables cần thiết:

1. **user_fcm_tokens**: Lưu FCM tokens của users
2. **notification_logs** (optional): Log các notifications đã gửi để tracking

```sql
CREATE TABLE notification_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(36) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    body TEXT,
    data_payload JSON,
    fcm_token VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    sent_at TIMESTAMP NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

---

## 🎯 Notification Types

### 1. Reward Notifications
- **Trigger**: Khi user nhận reward (addReward)
- **Content**: "Bạn nhận được X điểm cho badge Y"
- **Data**: rewardId, score, badge, reason

### 2. Challenge Progress Notifications
- **Trigger**: Khi challenge progress đạt milestones (50%, 75%, 100%)
- **Content**: "Bạn đã hoàn thành X% challenge Y"
- **Data**: challengeId, progress, target

### 3. Challenge Completed Notifications
- **Trigger**: Khi user hoàn thành challenge
- **Content**: "Chúc mừng! Bạn đã hoàn thành challenge X"
- **Data**: challengeId, reward (nếu có)

### 4. Leaderboard Notifications
- **Trigger**: Khi rank của user thay đổi đáng kể
- **Content**: "Bạn đã lên hạng X trong bảng xếp hạng"
- **Data**: leaderboardType, oldRank, newRank

---

## ⚙️ Configuration

### application.properties
```properties
# Firebase Configuration
firebase.service-account.path=classpath:firebase-service-account.json

# FCM Settings
fcm.enabled=true
fcm.batch-size=500
fcm.retry-attempts=3

# Notification Settings
notification.reward.enabled=true
notification.challenge.enabled=true
notification.leaderboard.enabled=true
```

---

## 🚀 Best Practices

### 1. Async Processing
- Gửi notification async để không block API response
- Sử dụng message queue (RabbitMQ, Kafka) nếu cần scale

### 2. Error Handling
- Log tất cả errors
- Retry mechanism cho failed notifications
- Dead letter queue cho notifications không thể gửi

### 3. Performance
- Batch notifications khi có thể
- Cache FCM tokens trong Redis
- Rate limiting để tránh spam

### 4. User Experience
- Cho phép user bật/tắt notifications theo loại
- Không gửi quá nhiều notifications trong thời gian ngắn
- Personalize notification content

---

## 📝 Checklist Implementation

### Backend:
- [ ] Thêm Firebase Admin SDK dependency
- [ ] Tạo FirebaseConfig
- [ ] Tạo UserFCMToken entity và repository
- [ ] Tạo FCMTokenService
- [ ] Tạo FCMTokenController
- [ ] Tạo FCMNotificationService
- [ ] Tích hợp vào RewardService
- [ ] Thêm error handling và logging
- [ ] Test với Firebase Console

### Frontend:
- [ ] Cài đặt Firebase SDK
- [ ] Tạo Firebase config
- [ ] Tạo service worker
- [ ] Implement requestPermission
- [ ] Implement saveToken API call
- [ ] Implement notification handler
- [ ] Test với browser console

### Testing:
- [ ] Test save token flow
- [ ] Test send notification từ backend
- [ ] Test receive notification ở frontend
- [ ] Test invalid token cleanup
- [ ] Test multiple devices

---

## 🔄 Next Steps

Sau khi hoàn thành basic implementation:

1. **Notification Preferences**: Cho phép user chọn loại notifications muốn nhận
2. **Notification History**: Lưu lịch sử notifications đã gửi
3. **Scheduled Notifications**: Gửi notifications theo lịch (ví dụ: daily summary)
4. **Rich Notifications**: Thêm images, actions buttons
5. **Analytics**: Track notification open rates, click rates

---

## 📚 Resources

- [Firebase Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)
- [Firebase Admin SDK for Java](https://firebase.google.com/docs/admin/setup)
- [FCM Web Setup Guide](https://firebase.google.com/docs/cloud-messaging/js/client)

