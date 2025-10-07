package vn.uth.gamificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GamificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GamificationServiceApplication.class, args);
    }

}
