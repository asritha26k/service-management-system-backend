package com.app.api_gateway.security;


public class JwtUserPrincipal {

    private final String userId;
    private final String role;
    private final String email;

    public JwtUserPrincipal(String userId, String role, String email) {
        this.userId = userId;
        this.role = role;
        this.email = email;
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
}
