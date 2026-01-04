package com.app.identity_service.seeder;

import com.app.identity_service.entity.UserAuth;
import com.app.identity_service.entity.UserRole;
import com.app.identity_service.repository.UserAuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DefaultUsersSeeder Component
 * Automatically seeds default MANAGER, CUSTOMER, and TECHNICIAN users at application startup in an idempotent manner.
 * The seeder checks if users with the configured emails already exist before creating them.
 * @Order(Integer.MAX_VALUE - 1) ensures this runs after AdminSeeder but before other components
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Integer.MAX_VALUE - 1)
public class DefaultUsersSeeder implements ApplicationRunner {

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            seedManager();
            seedCustomer();
            seedTechnician();
            log.info("✓ All default users seeded successfully");
        } catch (Exception e) {
            log.warn("⚠ Could not seed default users at startup (database may not be ready): {}", e.getMessage());
            log.info("You may need to create the users manually or ensure the database and tables exist before startup.");
        }
    }

    private void seedManager() {
        String managerEmail = "manager@example.com";
        String managerPassword = "manager@123";

        if (userAuthRepository.existsByEmail(managerEmail)) {
            log.info("Manager user with email '{}' already exists. Skipping seeding.", managerEmail);
            return;
        }

        UserAuth manager = new UserAuth();
        manager.setEmail(managerEmail);
        manager.setPassword(passwordEncoder.encode(managerPassword));
        manager.setRole(UserRole.MANAGER);
        manager.setIsActive(true);
        manager.setIsEmailVerified(true);
        manager.setForcePasswordChange(false);

        UserAuth savedManager = userAuthRepository.save(manager);
        log.info("✓ Manager user successfully seeded with email: {}", savedManager.getEmail());
    }

    private void seedCustomer() {
        String customerEmail = "customer@example.com";
        String customerPassword = "customer@123";

        if (userAuthRepository.existsByEmail(customerEmail)) {
            log.info("Customer user with email '{}' already exists. Skipping seeding.", customerEmail);
            return;
        }

        UserAuth customer = new UserAuth();
        customer.setEmail(customerEmail);
        customer.setPassword(passwordEncoder.encode(customerPassword));
        customer.setRole(UserRole.CUSTOMER);
        customer.setIsActive(true);
        customer.setIsEmailVerified(true);
        customer.setForcePasswordChange(false);

        UserAuth savedCustomer = userAuthRepository.save(customer);
        log.info("✓ Customer user successfully seeded with email: {}", savedCustomer.getEmail());
    }

    private void seedTechnician() {
        String technicianEmail = "technician@example.com";
        String technicianPassword = "technician@123";

        if (userAuthRepository.existsByEmail(technicianEmail)) {
            log.info("Technician user with email '{}' already exists. Skipping seeding.", technicianEmail);
            return;
        }

        UserAuth technician = new UserAuth();
        technician.setEmail(technicianEmail);
        technician.setPassword(passwordEncoder.encode(technicianPassword));
        technician.setRole(UserRole.TECHNICIAN);
        technician.setIsActive(true);
        technician.setIsEmailVerified(true);
        technician.setForcePasswordChange(false);

        UserAuth savedTechnician = userAuthRepository.save(technician);
        log.info("✓ Technician user successfully seeded with email: {}", savedTechnician.getEmail());
    }
}
