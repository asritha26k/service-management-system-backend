package com.app.notification_service.security;
public record RequestUser(
        String userId,
        String role
) {
    public boolean isAuthenticated() {
        return userId != null && !userId.isBlank();
    }
}
