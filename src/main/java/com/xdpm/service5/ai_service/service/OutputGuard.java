package com.xdpm.service5.ai_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OutputGuard {

    /**
     * Kiểm tra đầu ra: giới hạn độ dài, chặn URL hoặc từ khóa nhạy cảm.
     * Log chi tiết guard_pass=true/false và lý do.
     */
    public boolean validate(String text) {
        if (text == null || text.isBlank()) {
            log.warn("[GUARD] pass=false reason=empty_or_null");
            return false;
        }

        if (text.length() > 300) {
            log.warn("[GUARD] pass=false reason=text_too_long ({} chars)", text.length());
            return false;
        }

        String lower = text.toLowerCase();

        if (lower.contains("http") || lower.contains("www") || lower.contains("://")) {
            log.warn("[GUARD] pass=false reason=contains_url");
            return false;
        }

        if (lower.contains("bitcoin") || lower.contains("xxx") || lower.contains("lộ ảnh")) {
            log.warn("[GUARD] pass=false reason=sensitive_keyword_detected");
            return false;
        }

        log.info("[GUARD] pass=true text='{}'", truncate(text));
        return true;
    }

    /** Truncate text để log gọn, tránh tràn log khi output dài. */
    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 120 ? text.substring(0, 117) + "..." : text;
    }
}
