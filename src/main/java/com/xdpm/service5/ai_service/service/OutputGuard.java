package com.xdpm.service5.ai_service.service;

import org.springframework.stereotype.Component;

@Component
public class OutputGuard {

    /**
     * Kiểm tra đầu ra: giới hạn độ dài, chặn URL hoặc từ khóa nhạy cảm.
     */
    public boolean validate(String text) {
        if (text == null || text.length() > 300) return false;
        String lower = text.toLowerCase();
        if (lower.contains("http") || lower.contains("www") || lower.contains("://")) return false;
        if (lower.contains("bitcoin") || lower.contains("xxx") || lower.contains("lộ ảnh")) return false;
        return true;
    }
}

