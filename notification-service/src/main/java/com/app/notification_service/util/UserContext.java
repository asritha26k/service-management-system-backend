package com.app.notification_service.util;

import com.app.notification_service.exception.BadRequestException;

import java.util.EnumSet;
import java.util.Set;

public final class UserContext {

    private UserContext() {}

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    public enum Role {
        ADMIN, MANAGER, TECHNICIAN, CUSTOMER
    }

    public static void requireAuthenticated(String userId) {
        validateNotBlank(userId, "Unauthenticated request");
    }

    public static void requireRole(String userRole, Role... allowed) {
        Role role = parseRole(userRole);
        if (!EnumSet.of(allowed[0], allowed).contains(role)) {
            throw forbidden(role);
        }
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

        requireAnyRole(userRole, Role.ADMIN, Role.MANAGER);
    }

    public static boolean isAdminOrManager(String userRole) {
        return hasAnyRole(userRole, Role.ADMIN, Role.MANAGER);
    }

    private static void requireAnyRole(String userRole, Role... roles) {
        if (!hasAnyRole(userRole, roles)) {
            throw forbidden(parseRole(userRole));
        }
    }

    private static boolean hasAnyRole(String userRole, Role... roles) {
        try {
            Role role = parseRole(userRole);
            return Set.of(roles).contains(role);
        } catch (IllegalArgumentException | java.util.NoSuchElementException e) {
            return false;
        }
    }

    private static Role parseRole(String role) {
        validateNotBlank(role, "Missing user role");
        try {
            return Role.valueOf(role.toUpperCase().replace("ROLE_", ""));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + role);
        }
    }

    private static void validateNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private static BadRequestException forbidden(Role role) {
        return new BadRequestException("Forbidden for role: " + role);
    }
}
