package com.app.identity_service.seeder;

import com.app.identity_service.entity.UserAuth;
import com.app.identity_service.entity.UserRole;
import com.app.identity_service.repository.UserAuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// AdminSeeder Component
// Automatically seeds a single ADMIN user at application startup in an idempotent manner.
// The seeder checks if an admin with the configured email already exists before creating one.
// @Order(Integer.MAX_VALUE) ensures this runs after schema creation
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Integer.MAX_VALUE)
public class AdminSeeder implements ApplicationRunner {

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${admin.password:admin@123}")
    private String adminPassword;

    @Value("${admin.enabled:true}")
    private Boolean adminEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            // Idempotent check: if admin already exists, skip seeding
            if (userAuthRepository.existsByEmail(adminEmail)) {
                log.info("Admin user with email '{}' already exists. Skipping seeding.", adminEmail);
                return;
            }

            // Create new admin user
            UserAuth adminUser = new UserAuth();
            adminUser.setEmail(adminEmail);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setRole(UserRole.ADMIN);
            adminUser.setIsActive(adminEnabled);
            adminUser.setIsEmailVerified(true);
            adminUser.setForcePasswordChange(false);

            // Save the admin user
            UserAuth savedAdmin = userAuthRepository.save(adminUser);
            
            log.info("✓ Admin user successfully seeded with email: {}", savedAdmin.getEmail());
        } catch (Exception e) {
            log.warn("⚠ Could not seed admin user at startup (database may not be ready): {}", e.getMessage());
            log.info("You may need to create the admin user manually or ensure the database and tables exist before startup.");
            // Don't throw exception - allow application to start even if seeding fails
        }
    }
}
