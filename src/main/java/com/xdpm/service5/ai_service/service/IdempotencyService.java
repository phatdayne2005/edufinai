package com.xdpm.service5.ai_service.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private final StringRedisTemplate redis;

    /** @return true nếu set key thành công (chưa tồn tại); false nếu đã tồn tại => request trùng */
    public boolean tryAcquire(String key, Duration ttl) {
        Boolean ok = redis.opsForValue().setIfAbsent(buildKey(key), "1", ttl);
        boolean acquired = Boolean.TRUE.equals(ok);
        log.debug("Idempotency key={} acquired={}", key, acquired);
        return acquired;
    }

    public void releaseEarly(String key) { // optional
        redis.delete(buildKey(key));
    }

    private String buildKey(String raw) {
        return "idem:ai:" + raw;
    }
}
