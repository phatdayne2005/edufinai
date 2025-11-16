package vn.uth.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global Filter để tự động forward JWT token từ client request
 * xuống các microservice backend.
 * 
 * Lưu ý: Spring Cloud Gateway mặc định đã forward tất cả headers từ client
 * xuống backend service. Filter này được tạo để:
 * - Đảm bảo chắc chắn Authorization header được forward
 * - Có thể thêm logic validate/logging sau này nếu cần
 * - Có thể filter một số route cụ thể nếu cần
 * 
 * Cách hoạt động:
 * - Client gửi request với header: Authorization: Bearer <token>
 * - Gateway tự động forward header này xuống backend service
 * - Backend service validate JWT và xử lý request
 */
@Component
public class JwtTokenRelayFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Order của filter - chạy trước các filter khác
     * Số càng nhỏ, chạy càng sớm
     */
    @Override
    public int getOrder() {
        return -100; // Chạy sớm để đảm bảo header được forward
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Lấy Authorization header từ request của client
        List<String> authHeaders = exchange.getRequest()
                .getHeaders()
                .get(AUTHORIZATION_HEADER);

        // Log để debug (có thể bỏ sau khi test xong)
        if (authHeaders != null && !authHeaders.isEmpty()) {
            // Header đã có, Spring Cloud Gateway sẽ tự động forward
            // Không cần mutate request vì gateway đã làm sẵn
        }

        // Spring Cloud Gateway tự động forward tất cả headers từ client
        // xuống backend service, không cần thêm code
        return chain.filter(exchange);
    }
}
