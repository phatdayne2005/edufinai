package com.edufinai.auth.security;

import com.edufinai.auth.util.JwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommonAuthFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = parseJwt(request);

        if (token != null && jwtValidator.validateToken(token)) {
            // Token hợp lệ, có thể thêm thông tin user vào request attribute
            // để các service khác sử dụng
            try {
                String username = jwtValidator.extractUsername(token);
                request.setAttribute("username", username);
                request.setAttribute("userRole", jwtValidator.extractRole(token));
                // request.setAttribute("userId", jwtValidator.extractUserId(token));
            } catch (Exception e) {
                log.error("Error extracting user info from token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}