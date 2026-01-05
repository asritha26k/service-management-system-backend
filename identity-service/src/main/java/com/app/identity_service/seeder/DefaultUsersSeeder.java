package com.app.identity_service.seeder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

/**
 * DefaultUsersSeeder
 *
 * SECURITY NOTICE:
 * This seeder is intentionally DISABLED.
 *
 * Reason:
 * - Hardcoded or auto-seeded users are a security risk
 *
 * Recommended alternatives:
 * - Create users manually via admin APIs
 * - Use environment-specific scripts with externalized secrets
 *
 * To enable for LOCAL DEVELOPMENT ONLY:
 * - Reintroduce @Component
 * - Use secure, externalized credentials
 */
@Slf4j
// @Component // Intentionally disabled
@Order(Integer.MAX_VALUE - 1)
public class DefaultUsersSeeder implements ApplicationRunner {

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        log.warn(
                "DefaultUsersSeeder is disabled for security reasons. " +
                        "No default users were created at startup.");
    }
}
