package com.xdpm.service5.ai_service.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 🧩 IdempotencyService — Ngăn trùng lặp request hoặc event.
 * Lưu key tạm thời trên Redis với TTL (time-to-live).
 * Dùng cho: /api/v1/ai/recommend, /dev/event ...
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private final StringRedisTemplate redis;

    // --------------------------------------------------------
    // 1️⃣ Thử acquire key (true = chưa từng dùng, false = trùng)
    // --------------------------------------------------------
    public boolean tryAcquire(String key, Duration ttl) {
        String redisKey = buildKey(key);
        Boolean success = redis.opsForValue().setIfAbsent(redisKey, "LOCKED", ttl.getSeconds(), TimeUnit.SECONDS);
        boolean acquired = Boolean.TRUE.equals(success);

        log.info("[Idempotency] key={} acquired={} (TTL={}s)", key, acquired, ttl.getSeconds());
        return acquired;
    }

    // ✅ Alias cho tương thích backward (nếu service khác gọi acquire)
    public boolean acquire(String key, long ttlSeconds) {
        return tryAcquire(key, Duration.ofSeconds(ttlSeconds));
    }

    // --------------------------------------------------------
    // 2️⃣ Lưu kết quả xử lý (ví dụ lưu ID khuyến nghị vừa tạo)
    // --------------------------------------------------------
    public void storeResult(String key, String aiId, long ttlSeconds) {
        String redisKey = buildKey(key) + ":result";
        redis.opsForValue().set(redisKey, aiId, ttlSeconds, TimeUnit.SECONDS);
        log.debug("[Idempotency] Stored result key={} -> aiId={} (TTL={}s)", key, aiId, ttlSeconds);
    }

    // --------------------------------------------------------
    // 3️⃣ Lấy kết quả cũ nếu đã xử lý trước đó
    // --------------------------------------------------------
    public Optional<String> getSavedResult(String key) {
        String redisKey = buildKey(key) + ":result";
        String value = redis.opsForValue().get(redisKey);
        log.debug("[Idempotency] getSavedResult key={} found={}", key, value != null);
        return Optional.ofNullable(value);
    }

    // --------------------------------------------------------
    // 4️⃣ Giải phóng sớm (optional)
    // --------------------------------------------------------
    public void releaseEarly(String key) {
        redis.delete(buildKey(key));
        log.debug("[Idempotency] Released early key={}", key);
    }

    // --------------------------------------------------------
    // 5️⃣ Helper
    // --------------------------------------------------------
    private String buildKey(String raw) {
        return "idem:ai:" + raw;
    }
}
