package vn.uth.edufinai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Utility class cho WebClient operations
 */
@Slf4j
public final class WebClientUtils {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private WebClientUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Fetch JSON data từ URL với JWT authentication
     * 
     * @param webClient WebClient instance
     * @param url URL để fetch
     * @param jwtAuth JWT authentication token
     * @return Mono<Map<String, Object>> chứa JSON data, hoặc empty map nếu lỗi
     */
    public static Mono<Map<String, Object>> fetchUserScopedJson(
            WebClient webClient,
            String url,
            JwtAuthenticationToken jwtAuth) {
        return fetchUserScopedJson(webClient, url, jwtAuth, DEFAULT_TIMEOUT);
    }

    /**
     * Fetch JSON data từ URL với JWT authentication và custom timeout
     * 
     * @param webClient WebClient instance
     * @param url URL để fetch
     * @param jwtAuth JWT authentication token
     * @param timeout Timeout duration
     * @return Mono<Map<String, Object>> chứa JSON data, hoặc empty map nếu lỗi
     */
    public static Mono<Map<String, Object>> fetchUserScopedJson(
            WebClient webClient,
            String url,
            JwtAuthenticationToken jwtAuth,
            Duration timeout) {
        if (url == null || url.isBlank()) {
            return Mono.just(Map.of());
        }

        return webClient
                .get()
                .uri(url)
                .headers(headers -> headers.setBearerAuth(jwtAuth.getToken().getTokenValue()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(timeout)
                .onErrorResume(ex -> {
                    log.warn("Downstream service unavailable: url={}, error={}", url, ex.getMessage());
                    return Mono.just(Map.of());
                });
    }
}

