package com.app.service_operations_service.util;

import com.app.service_operations_service.exception.BadRequestException;

public final class UserContext {

    private UserContext() {}

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    public enum Role {
        ADMIN, MANAGER, TECHNICIAN, CUSTOMER
    }

    // ===================== AUTH =====================

    public static void requireAuthenticated(String userId) {
        if (isBlank(userId)) {
            throw new BadRequestException("Unauthenticated request");
        }
    }

    public static void requireRole(String userRole, Role... allowed) {
        Role role = parseRole(userRole);

        for (Role r : allowed) {
            if (r == role) {
                return;
            }
        }

        throw new BadRequestException("Forbidden for role: " + role);
    }

    public static void requireOwnershipOrAdmin(
            String userId,
            String userRole,
            String resourceOwnerId
    ) {
        requireAuthenticated(userId);

        if (userId.equals(resourceOwnerId)) {
            return;
        }

        Role role = parseRole(userRole);
        if (role == Role.ADMIN || role == Role.MANAGER) {
            return;
        }

        throw new BadRequestException("Access denied");
    }

    // ===================== HELPERS =====================

    public static boolean isAdminOrManager(String userRole) {
        try {
            Role r = parseRole(userRole);
            return r == Role.ADMIN || r == Role.MANAGER;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Role parseRole(String role) {
        if (isBlank(role)) {
            throw new BadRequestException("Missing user role");
        }

        try {
            return Role.valueOf(role.toUpperCase().replace("ROLE_", ""));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + role);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
