package com.app.technicianservice.security;
public record RequestUser(
        String userId,
        String role
) {
    public boolean isAuthenticated() {
        return userId != null && !userId.isBlank();
    }
}
