package com.xdpm.service5.ai_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest  // ← chạy full context (web, DB, bean, ...)
class AiServiceApplicationTests {

    @Test
    void contextLoads() {
        // Test rất đơn giản: chỉ cần ứng dụng khởi động OK
        // Nếu @SpringBootApplication bị lỗi config → test này fail.
    }
}
