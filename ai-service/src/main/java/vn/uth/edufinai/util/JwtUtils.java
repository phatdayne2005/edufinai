package vn.uth.edufinai.util;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Utility class cho JWT operations
 */
public final class JwtUtils {

    private JwtUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Extract JWT token từ Authentication và validate
     * 
     * @param authentication Authentication object từ Spring Security
     * @return JwtAuthenticationToken nếu valid
     * @throws ResponseStatusException nếu không phải JWT token
     */
    public static JwtAuthenticationToken requireJwt(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT token is required");
        }
        return jwtAuth;
    }

    /**
     * Extract JWT token từ Authentication và validate (reactive)
     * 
     * @param authentication Authentication object từ Spring Security
     * @return Mono<JwtAuthenticationToken> nếu valid
     */
    public static Mono<JwtAuthenticationToken> requireJwtMono(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT token is required"));
        }
        return Mono.just(jwtAuth);
    }

    /**
     * Extract userId từ JWT token
     * 
     * @param jwtAuth JWT authentication token
     * @return userId từ token subject hoặc name
     */
    public static String extractUserId(JwtAuthenticationToken jwtAuth) {
        String subject = jwtAuth.getToken().getSubject();
        return subject != null ? subject : jwtAuth.getName();
    }

    /**
     * Extract userId từ Authentication (combines requireJwt + extractUserId)
     * 
     * @param authentication Authentication object
     * @return userId
     * @throws ResponseStatusException nếu không phải JWT token
     */
    public static String extractUserId(Authentication authentication) {
        JwtAuthenticationToken jwtAuth = requireJwt(authentication);
        return extractUserId(jwtAuth);
    }
}

