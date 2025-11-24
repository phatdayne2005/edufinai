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

        log.debug("Fetching data from: url={}", url);
        return webClient
                .get()
                .uri(url)
                .headers(headers -> headers.setBearerAuth(jwtAuth.getToken().getTokenValue()))
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    log.error("Downstream service returned error status: url={}, status={}", url, response.statusCode());
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(errorBody -> {
                                log.error("Error response body: {}", errorBody);
                                return Mono.error(new RuntimeException("Service returned " + response.statusCode() + ": " + errorBody));
                            });
                })
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(timeout)
                .doOnNext(data -> {
                    if (data == null || data.isEmpty()) {
                        log.warn("Downstream service returned empty data: url={}", url);
                    } else {
                        log.info("Successfully fetched data from: url={}, dataSize={}, keys={}", 
                                url, data.size(), data.keySet());
                    }
                })
                .doOnError(ex -> {
                    log.error("Error fetching from downstream service: url={}, error={}, errorType={}", 
                            url, ex.getMessage(), ex.getClass().getSimpleName(), ex);
                })
                .onErrorResume(ex -> {
                    log.error("Downstream service unavailable: url={}, error={}, errorType={}", 
                            url, ex.getMessage(), ex.getClass().getSimpleName());
                    return Mono.just(Map.of());
                });
    }
}

