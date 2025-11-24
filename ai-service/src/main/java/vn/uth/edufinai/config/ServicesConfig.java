package vn.uth.edufinai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class để bind nested properties từ application.yaml
 * 
 * Usage:
 * - Inject ServicesConfig vào controller
 * - Access: servicesConfig.getFinance().getUrl()
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServicesConfig {
    
    private Service finance = new Service();
    private Service learning = new Service();
    private Service gamification = new Service();
    
    @Data
    public static class Service {
        private String url;
    }
}

