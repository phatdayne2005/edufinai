package vn.uth.edufinai.configuration;

import java.util.HashSet;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.uth.edufinai.constant.PredefinedRole;
import vn.uth.edufinai.entity.Role;
import vn.uth.edufinai.entity.User;
import vn.uth.edufinai.repository.RoleRepository;
import vn.uth.edufinai.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @NonFinal
    static final String ADMIN_USER_NAME = "admin";

    @NonFinal
    static final String ADMIN_PASSWORD = "admin";

    @Bean
    @ConditionalOnProperty(
            prefix = "spring",
            value = "datasource.driverClassName",
            havingValue = "com.mysql.cj.jdbc.Driver")
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        log.info("Initializing application.....");
        return args -> {

            // Đảm bảo luôn có đủ 4 role trong DB
            createRoleIfNotExists(roleRepository, PredefinedRole.LEARNER_ROLE,
                    "Learner / Young user role");
            createRoleIfNotExists(roleRepository, PredefinedRole.CREATOR_ROLE,
                    "Content creator / Finance educator role");
            createRoleIfNotExists(roleRepository, PredefinedRole.MOD_ROLE,
                    "Content moderator role");
            createRoleIfNotExists(roleRepository, PredefinedRole.ADMIN_ROLE,
                    "Administrator role");

            // Chỉ tạo user admin mặc định khi chưa tồn tại
            if (userRepository.findByUsername(ADMIN_USER_NAME).isEmpty()) {
                Role adminRole = roleRepository.findById(PredefinedRole.ADMIN_ROLE)
                        .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

                var roles = new HashSet<Role>();
                roles.add(adminRole);

                User user = User.builder()
                        .username(ADMIN_USER_NAME)
                        .password(passwordEncoder.encode(ADMIN_PASSWORD))
                        .email("admin@edufinai.local")
                        .phone("0000000000")
                        .roles(roles)
                        .build();

                userRepository.save(user);
                log.warn("admin user has been created with default password: admin, please change it");
            }

            log.info("Application initialization completed .....");
        };
    }

    private void createRoleIfNotExists(RoleRepository roleRepository, String roleName, String description) {
        roleRepository.findById(roleName).orElseGet(() -> {
            log.info("Seeding role: {}", roleName);
            return roleRepository.save(
                    Role.builder()
                            .name(roleName)
                            .description(description)
                            .build()
            );
        });
    }
}
