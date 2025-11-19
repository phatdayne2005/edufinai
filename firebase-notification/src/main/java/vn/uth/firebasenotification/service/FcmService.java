package vn.uth.firebasenotification.service;

import com.google.common.collect.Lists;
import com.google.firebase.messaging.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.uth.firebasenotification.entity.FcmToken;
import vn.uth.firebasenotification.repository.FcmTokenRepository;

import java.security.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class FcmService {

    private final FcmTokenRepository tokenRepo;
    private final String defaultTopic;
    private static final int MULTICAST_BATCH_SIZE = 500;

    public FcmService(FcmTokenRepository tokenRepo,
                      @Value("${fcm.default-topic:all}") String defaultTopic) {
        this.tokenRepo = tokenRepo;
        this.defaultTopic = defaultTopic;
    }

    // Save token (register)
    @Transactional
    public void registerToken(Long userId, String token, String platform, String deviceInfo) {
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
        try { subscribeTokenToTopic(token, defaultTopic); } catch (Exception ignored) {}
    }

    public void removeToken(Long userId, String token) {
        tokenRepo.findByToken(token).ifPresent(t -> {
            if (t.getUserId().equals(userId)) {
                tokenRepo.deactivateByToken(token);
                try { unsubscribeTokenFromTopic(token, defaultTopic); } catch (Exception ignored) {}
            }
        });
    }

    // Send single notification to a token
    public void sendToToken(String token, String title, String body, Map<String,String> data) {
        Message msg = Message.builder()
                .setToken(token)
                .setNotification(new Notification(title, body))
                .putAllData(data != null ? data : Collections.emptyMap())
                .build();
        try {
            String resp = FirebaseMessaging.getInstance().send(msg);
            // success: update last_seen maybe
            tokenRepo.findByToken(token).ifPresent(t -> {
                t.setLastSeen(new Timestamp(System.currentTimeMillis()));
                tokenRepo.save(t);
            });
        } catch (FirebaseMessagingException e) {
            handleFcmExceptionForToken(e, token);
        }
    }

    // Multicast to list of tokens (batching)
    public void sendToTokens(List<String> tokens, String title, String body, Map<String,String> data) {
        List<List<String>> batches = Lists.partition(tokens, MULTICAST_BATCH_SIZE);
        for (List<String> batch : batches) {
            MulticastMessage mm = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .setNotification(new Notification(title, body))
                    .putAllData(data != null ? data : Collections.emptyMap())
                    .build();
            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(mm);
                // handle responses: remove invalid tokens
                for (int i=0;i<response.getResponses().size();i++) {
                    SendResponse r = response.getResponses().get(i);
                    String tkn = batch.get(i);
                    if (!r.isSuccessful()) {
                        FirebaseMessagingException ex = (FirebaseMessagingException) r.getException();
                        handleFcmExceptionForToken(ex, tkn);
                    } else {
                        tokenRepo.findByToken(tkn).ifPresent(tok -> {
                            tok.setLastSeen(new Timestamp(System.currentTimeMillis()));
                            tokenRepo.save(tok);
                        });
                    }
                }
            } catch (FirebaseMessagingException ex) {
                // transient or top-level error - consider retry later
                // log and optionally requeue job
            }
        }
    }

    // Send to topic
    public void sendToTopic(String topic, String title, String body, Map<String,String> data) {
        Message msg = Message.builder()
                .setTopic(topic)
                .setNotification(new Notification(title, body))
                .putAllData(data != null ? data : Collections.emptyMap())
                .build();
        try {
            String resp = FirebaseMessaging.getInstance().send(msg);
        } catch (FirebaseMessagingException e) {
            // handle top-level errors
        }
    }

    // Subscribe token(s) to a topic
    public void subscribeTokenToTopic(String token, String topic) throws FirebaseMessagingException {
        TopicManagementResponse resp = FirebaseMessaging.getInstance().subscribeToTopic(Collections.singletonList(token), topic);
        // check resp.getErrors() for failures
    }

    public void unsubscribeTokenFromTopic(String token, String topic) throws FirebaseMessagingException {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Collections.singletonList(token), topic);
    }

    private void handleFcmExceptionForToken(FirebaseMessagingException e, String token) {
        String code = e.getErrorCode(); // e.g., "registration-token-not-registered" or other
        if (code != null && code.contains("registration-token-not-registered")) {
            // delete token from DB
            tokenRepo.findByToken(token).ifPresent(t -> tokenRepo.delete(t));
        } else {
            // log other errors; maybe retry if transient
        }
    }

    // Helper: send notification to a userId (all his active tokens)
    public void sendToUser(Long userId, String title, String body, Map<String,String> data) {
        List<FcmToken> tokens = tokenRepo.findByUserIdAndIsActiveTrue(userId);
        List<String> tks = tokens.stream().map(FcmToken::getToken).collect(Collectors.toList());
        if (!tks.isEmpty()) sendToTokens(tks, title, body, data);
    }

    // Broadcast: either publish to topic or batch send to all tokens
    public void broadcastToAll(String title, String body, Map<String,String> data) {
        // Option A: send to a topic "all"
        sendToTopic(defaultTopic, title, body, data);

        // Option B: fallback: iterate tokens in DB in batches and send multicast
        // (uncomment to use)
        // List<String> allTokens = tokenRepo.findAllActiveTokens(); // implement repo method
        // sendToTokens(allTokens, title, body, data);
    }
}
