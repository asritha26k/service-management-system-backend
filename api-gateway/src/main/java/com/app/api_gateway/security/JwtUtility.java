package com.app.api_gateway.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Handles token validation and claims extraction
@Component
public class JwtUtility {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtility.class);
    private static final String CLAIM_NEEDS_PW_CHANGE = "needsPasswordChange";

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Validate JWT token
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            logger.debug("✓ Token validated successfully");
            return true;
        } catch (Exception e) {
            logger.warn("✗ Token validation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    // Extract all claims
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extract email (subject)
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extract user id
    public String extractUserId(String token) {
        return extractAllClaims(token).get("userId", String.class);
    }

    // Extract role
    public String extractRole(String token) {
        String role = extractAllClaims(token).get("role", String.class);
        logger.debug("Extracted role from token: {}", role);
        return role;
    }

    public boolean extractNeedsPasswordChange(String token) {
        Boolean needsChange = extractAllClaims(token).get(CLAIM_NEEDS_PW_CHANGE, Boolean.class);
        return needsChange != null && needsChange;
    }
}
