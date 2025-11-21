package vn.uth.firebasenotification.service;

import com.google.common.collect.Lists;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.TopicManagementResponse;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.uth.firebasenotification.entity.FcmToken;
import vn.uth.firebasenotification.repository.FcmTokenRepository;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);
    private final FcmTokenRepository tokenRepo;
    private final String defaultTopic;
    private static final int MULTICAST_BATCH_SIZE = 500;
    private final UserService userService;

    public FcmService(FcmTokenRepository tokenRepo,
                      @Value("${fcm.default-topic:all}") String defaultTopic, UserService userService) {
        this.tokenRepo = tokenRepo;
        this.defaultTopic = defaultTopic;
        this.userService = userService;
    }

    // Save token (register)
    @Transactional
    public void registerToken(UUID userId, String token, String platform, String deviceInfo) {
        FcmToken existing = tokenRepo.findByToken(token).orElse(null);
        if (existing != null) {
            existing.setIsActive(true);
            existing.setUserId(userId);
            existing.setPlatform(platform);
            existing.setDeviceInfo(deviceInfo);
            tokenRepo.save(existing);
        } else {
            FcmToken t = new FcmToken();
            t.setUserId(userId);
            t.setToken(token);
            t.setPlatform(platform);
            t.setDeviceInfo(deviceInfo);
            tokenRepo.save(t);
        }
        // Optionally subscribe to default topic:
        try {
            subscribeTokenToTopic(token, defaultTopic);
        } catch (Exception ignored) {
        }
    }

    public void removeToken(UUID userId, String token) {
        tokenRepo.findByToken(token).ifPresent(t -> {
            if (t.getUserId().equals(userId)) {
                tokenRepo.deactivateByToken(token);
                try {
                    unsubscribeTokenFromTopic(token, defaultTopic);
                } catch (Exception ignored) {
                }
            }
        });
    }

    // Send single notification to a token
    public void sendToToken(String token, String title, String body, Map<String, String> data) {
        Message.Builder builder = Message.builder()
                .setToken(token)
                .putAllData(data != null ? data : Collections.emptyMap());

        if (hasNotificationPayload(title, body)) {
            builder.setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build());
        }

        Message msg = builder.build();
        try {
            FirebaseMessaging.getInstance().send(msg);
            // success: update last_seen maybe
            tokenRepo.findByToken(token).ifPresent(t -> {
                t.setLastSeen(new Timestamp(System.currentTimeMillis()));
                tokenRepo.save(t);
            });
        } catch (FirebaseMessagingException e) {
            log.warn("FCM send to token failed: token={}, errorCode={}, message={}",
                    token, e.getMessagingErrorCode(), e.getMessage());
            handleFcmExceptionForToken(e, token);
        }
    }

    // Gửi tuần tự từng token để tránh lỗi mạng / batch
    public void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
        for (String token : tokens) {
            sendToToken(token, title, body, data);
        }
    }

    // Send to topic
    public void sendToTopic(String topic, String title, String body, Map<String, String> data) {
        Message.Builder builder = Message.builder()
                .setTopic(topic)
                .putAllData(data != null ? data : Collections.emptyMap());

        if (hasNotificationPayload(title, body)) {
            builder.setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build());
        }

        Message msg = builder.build();
        try {
            FirebaseMessaging.getInstance().send(msg);
        } catch (FirebaseMessagingException e) {
            log.error("FCM topic send failed: topic={}, errorCode={}, message={}",
                    topic, e.getMessagingErrorCode(), e.getMessage());
        }
    }

    // Subscribe token(s) to a topic
    public void subscribeTokenToTopic(String token, String topic) throws FirebaseMessagingException {
        TopicManagementResponse resp = FirebaseMessaging.getInstance()
                .subscribeToTopic(Collections.singletonList(token), topic);
        if (!resp.getErrors().isEmpty()) {
            // TODO: handle individual subscription failures (log/alert)
        }
    }

    public void unsubscribeTokenFromTopic(String token, String topic) throws FirebaseMessagingException {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Collections.singletonList(token), topic);
    }

    private void handleFcmExceptionForToken(FirebaseMessagingException e, String token) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (MessagingErrorCode.UNREGISTERED.equals(code)) {
            // delete token from DB
            tokenRepo.findByToken(token).ifPresent(t -> tokenRepo.delete(t));
            log.info("Removed unregistered FCM token {}", token);
        } else {
            // log other errors; maybe retry if transient
            log.warn("FCM send failed for token {} with error {}", token, code);
        }
    }

    // Helper: send notification to a userId (all his active tokens)
    public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        List<FcmToken> tokens = tokenRepo.findByUserIdAndIsActiveTrue(userId);
        List<String> tks = tokens.stream().map(FcmToken::getToken).collect(Collectors.toList());
        if (tks.isEmpty()) {
            log.warn("No active FCM tokens found for user {}", userId);
            return;
        }
        log.info("Sending notification to user {} with {} tokens", userId, tks.size());
        sendToTokens(tks, title, body, data);
    }

    // Broadcast: either publish to topic or batch send to all tokens
    public void broadcastToAll(String title, String body, Map<String, String> data) {
        // Option A: send to a topic "all"
        sendToTopic(defaultTopic, title, body, data);

        // Option B: fallback: iterate tokens in DB in batches and send multicast
        // (uncomment to use)
        // List<String> allTokens = tokenRepo.findAllActiveTokens(); // implement repo
        // method
        // sendToTokens(allTokens, title, body, data);
    }

    private boolean hasNotificationPayload(String title, String body) {
        return (title != null && !title.isBlank()) || (body != null && !body.isBlank());
    }
}
