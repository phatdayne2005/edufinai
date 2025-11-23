package vn.uth.financeservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class SpringDataWebConfig {
    // Configuration class để enable stable JSON serialization cho Page objects
    // Sử dụng VIA_DTO mode để đảm bảo JSON structure ổn định
}

