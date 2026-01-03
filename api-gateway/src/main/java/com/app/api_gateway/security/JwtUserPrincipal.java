package com.app.api_gateway.security;

public class JwtUserPrincipal {

    private final String userId;
    private final String role;
    private final String email;
    private final boolean needsPasswordChange;

    public JwtUserPrincipal(String userId, String role, String email, boolean needsPasswordChange) {
        this.userId = userId;
        this.role = role;
        this.email = email;
        this.needsPasswordChange = needsPasswordChange;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public boolean isNeedsPasswordChange() {
        return needsPasswordChange;
    }
}
