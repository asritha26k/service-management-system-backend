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

/**
 * AdminSeeder Component
 * 
 * Automatically seeds a single ADMIN user at application startup in an
 * idempotent manner.
 * The seeder checks if an admin with the configured email already exists before
 * creating one.
 * 
 * SECURITY NOTICE:
 * Admin credentials MUST be configured via environment variables or secure
 * configuration:
 * - admin.email: Email address for the admin user
 * - admin.password: Strong password for the admin user (REQUIRED - no default
 * for security)
 * - admin.enabled: Whether to enable admin seeding (default: true)
 * 
 * For production environments, ensure:
 * 1. Use strong, unique passwords from secure secret management systems
 * 2. Never commit credentials to source control
 * 3. Consider disabling auto-seeding (admin.enabled=false) and create admin
 * manually
 * 
 * @Order(Integer.MAX_VALUE) ensures this runs after schema creation
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Integer.MAX_VALUE)
public class AdminSeeder implements ApplicationRunner {

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    @Value("${admin.enabled:true}")
    private Boolean adminEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            // Security validation: ensure credentials are configured
            if (!adminEnabled) {
                log.info("Admin seeding is disabled (admin.enabled=false). Skipping admin user creation.");
                return;
            }

            if (adminEmail == null || adminEmail.trim().isEmpty()) {
                log.warn("⚠ SECURITY: Admin email not configured. Skipping admin seeding.");
                log.info("Set 'admin.email' property to enable admin user creation.");
                return;
            }

            if (adminPassword == null || adminPassword.trim().isEmpty()) {
                log.warn("⚠ SECURITY: Admin password not configured. Skipping admin seeding.");
                log.info("Set 'admin.password' property to enable admin user creation.");
                return;
            }

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
            log.info(
                    "You may need to create the admin user manually or ensure the database and tables exist before startup.");
            // Don't throw exception - allow application to start even if seeding fails
        }
    }
}
