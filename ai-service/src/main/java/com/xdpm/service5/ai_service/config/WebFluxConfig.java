package com.xdpm.service5.ai_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        // Đảm bảo UTF-8 encoding cho request/response
        configurer.defaultCodecs().enableLoggingRequestDetails(false);
    }
}

