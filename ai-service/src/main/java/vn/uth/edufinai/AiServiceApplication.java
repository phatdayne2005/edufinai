package vn.uth.edufinai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import vn.uth.edufinai.config.ServicesConfig;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(ServicesConfig.class)
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}

