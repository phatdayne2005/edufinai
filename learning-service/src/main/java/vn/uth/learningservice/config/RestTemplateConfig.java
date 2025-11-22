package vn.uth.learningservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    // Bean RestTemplate có Load Balancer (dùng serviceId từ Eureka)
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3_000);
        requestFactory.setReadTimeout(5_000);

        RestTemplate restTemplate = builder
                .requestFactory(() -> requestFactory)
                .build();

        // Interceptor forward JWT hiện tại sang service khác
        restTemplate.getInterceptors().add((request, body, execution) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                String token = jwtAuth.getToken().getTokenValue();
                request.getHeaders().setBearerAuth(token);
            } else if (auth != null && auth.getCredentials() instanceof String tokenStr) {
                request.getHeaders().setBearerAuth(tokenStr);
            }

            return execution.execute(request, body);
        });

        return restTemplate;
    }

    @Bean
    public ClientHttpRequestInterceptor jwtForwardingInterceptor() {
        return (request, body, execution) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null) {
                return execution.execute(request, body);
            }

            // Trường hợp bạn dùng Spring Security Resource Server (JwtAuthenticationToken)
            if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
                String tokenValue = jwtAuth.getToken().getTokenValue();
                request.getHeaders().setBearerAuth(tokenValue);
            }
            // Hoặc nếu bạn tự lưu token trong credentials (String)
            else if (auth.getCredentials() instanceof String tokenStr) {
                request.getHeaders().setBearerAuth(tokenStr);
            }

            return execution.execute(request, body);
        };
    }
}

