package vn.uth.edufinai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(1)
@Slf4j
public class RequestLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        String remoteAddress = getRemoteAddress(exchange);
        
        if (log.isDebugEnabled()) {
            log.debug(">>> REQUEST: {} {} from {}", method, path, remoteAddress);
        }
        
        return chain.filter(exchange)
                .doOnSuccess(aVoid -> {
                    if (log.isDebugEnabled()) {
                        int statusCode = getStatusCode(exchange);
                        log.debug("<<< RESPONSE: {} {} - Status: {}", method, path, statusCode);
                    }
                })
                .doOnError(throwable -> {
                    log.error("<<< ERROR: {} {} - {}", method, path, throwable.getMessage());
                });
    }
    
    private String getRemoteAddress(ServerWebExchange exchange) {
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";
    }
    
    private int getStatusCode(ServerWebExchange exchange) {
        var statusCode = exchange.getResponse().getStatusCode();
        return statusCode != null ? statusCode.value() : 0;
    }
}

