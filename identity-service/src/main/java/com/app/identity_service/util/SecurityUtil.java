package com.app.identity_service.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Security Utility Class
// Provides helper methods for extracting user information from security context
@Component
public class SecurityUtil {

    // Extract userId from SecurityContext (stored in Authentication details)
    public String extractUserIdFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getDetails() instanceof String userId) {
            return userId;
        }
        return null;
    }


    // Extract email (username) from SecurityContext
    public String extractEmailFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    // Extract role from SecurityContext
    public String extractRoleFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse(null);
        }
        return null;
    }
}
