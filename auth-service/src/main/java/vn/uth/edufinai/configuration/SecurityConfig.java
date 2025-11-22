package vn.uth.edufinai.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
// CORS imports - đã tắt CORS filter vì Gateway xử lý CORS
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
// import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    // Public endpoints - match với path sau context path (/identity)
    // Gateway forward: /auth/users -> /identity/users
    // SecurityConfig match: /users (sau khi remove context path /identity)
    private final String[] PUBLIC_ENDPOINTS = {
            "/users", "/auth/token", "/auth/introspect", "/auth/logout", "/auth/refresh",
            "/auth/forgot-password", "/auth/verify-otp", "/auth/reset-password"
    };

    @Autowired
    private CustomJwtDecoder customJwtDecoder;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        // Cho phép POST đến các public endpoints (không cần authentication)
        // Spring Security tự động xử lý context path, nên chỉ cần path sau context path
        httpSecurity.authorizeHttpRequests(request -> request
                .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS)
                .permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**")  // Cho phép OPTIONS cho CORS preflight
                .permitAll()
                .anyRequest()
                .authenticated());

        httpSecurity.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtConfigurer -> jwtConfigurer
                .decoder(customJwtDecoder)
                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    /**
     * CORS Filter - ĐÃ TẮT
     * 
     * Lý do: Vì request đi qua Gateway, Gateway đã xử lý CORS rồi.
     * Nếu Auth Service cũng thêm CORS headers sẽ gây duplicate headers:
     * "Access-Control-Allow-Origin: http://localhost:3000, http://localhost:3000"
     * 
     * Nếu cần gọi trực tiếp Auth Service (không qua Gateway), uncomment bean này.
     */
    // @Bean
    // public CorsFilter corsFilter() {
    // CorsConfiguration corsConfiguration = new CorsConfiguration();
    // corsConfiguration.addAllowedOrigin("http://localhost:3000");
    // corsConfiguration.addAllowedOrigin("http://localhost:8080");
    // corsConfiguration.addAllowedMethod("*");
    // corsConfiguration.addAllowedHeader("*");
    // corsConfiguration.setAllowCredentials(true);
    //
    // UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new
    // UrlBasedCorsConfigurationSource();
    // urlBasedCorsConfigurationSource.registerCorsConfiguration("/**",
    // corsConfiguration);
    //
    // return new CorsFilter(urlBasedCorsConfigurationSource);
    // }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
