package com.app.identity_service.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilityTest {

    @InjectMocks
    private JwtUtility jwtUtility;

    private static final String TEST_SECRET = "test-secret-key-for-testing-purposes-only-must-be-at-least-256-bits-long-for-hmac-sha-256-algorithm";
    private static final long ACCESS_TOKEN_EXPIRY = 3600000L; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRY = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtility, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtility, "accessTokenExpiry", ACCESS_TOKEN_EXPIRY);
        ReflectionTestUtils.setField(jwtUtility, "refreshTokenExpiry", REFRESH_TOKEN_EXPIRY);
    }

    @Test
    void generateAccessToken_ShouldGenerateValidToken() {
        String token = jwtUtility.generateAccessToken("user-1", "user@example.com", "CUSTOMER");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtUtility.validateToken(token));
    }

    @Test
    void generateAccessToken_ShouldContainCorrectClaims() {
        String token = jwtUtility.generateAccessToken("user-1", "user@example.com", "CUSTOMER");

        Claims claims = jwtUtility.extractAllClaims(token);
        assertEquals("user-1", claims.get("userId", String.class));
        assertEquals("user@example.com", claims.getSubject());
        assertEquals("user@example.com", claims.get("email", String.class));
        assertEquals("CUSTOMER", claims.get("role", String.class));
    }

    @Test
    void generateRefreshToken_ShouldGenerateValidToken() {
        String token = jwtUtility.generateRefreshToken("user-1", "user@example.com");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtUtility.validateToken(token));
    }

    @Test
    void generateRefreshToken_ShouldContainCorrectClaims() {
        String token = jwtUtility.generateRefreshToken("user-1", "user@example.com");

        Claims claims = jwtUtility.extractAllClaims(token);
        assertEquals("user-1", claims.get("userId", String.class));
        assertEquals("user@example.com", claims.getSubject());
        assertEquals("REFRESH", claims.get("type", String.class));
    }

    @Test
    void extractEmail_ShouldExtractEmailFromToken() {
        String token = jwtUtility.generateAccessToken("user-1", "user@example.com", "CUSTOMER");

        String email = jwtUtility.extractEmail(token);

        assertEquals("user@example.com", email);
    }

    @Test
    void extractUserId_ShouldExtractUserIdFromToken() {
        String token = jwtUtility.generateAccessToken("user-1", "user@example.com", "CUSTOMER");

        String userId = jwtUtility.extractUserId(token);

        assertEquals("user-1", userId);
    }

    @Test
    void extractRole_ShouldExtractRoleFromToken() {
        String token = jwtUtility.generateAccessToken("user-1", "user@example.com", "CUSTOMER");

        String role = jwtUtility.extractRole(token);

        assertEquals("CUSTOMER", role);
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {
        String token = jwtUtility.generateAccessToken("user-1", "user@example.com", "CUSTOMER");

        boolean isValid = jwtUtility.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_ForInvalidToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtUtility.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_ForEmptyToken() {
        boolean isValid = jwtUtility.validateToken("");

        assertFalse(isValid);
    }

    @Test
    void isTokenExpired_ShouldReturnFalse_ForNonExpiredToken() {
        String token = jwtUtility.generateAccessToken("user-1", "user@example.com", "CUSTOMER");

        boolean isExpired = jwtUtility.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    void getRemainingExpiryTime_ShouldReturnPositiveValue_ForValidToken() {
        String token = jwtUtility.generateAccessToken("user-1", "user@example.com", "CUSTOMER");

        long remainingTime = jwtUtility.getRemainingExpiryTime(token);

        assertTrue(remainingTime > 0);
        assertTrue(remainingTime <= ACCESS_TOKEN_EXPIRY);
    }

    @Test
    void getRemainingExpiryTime_ShouldReturnZero_ForInvalidToken() {
        long remainingTime = jwtUtility.getRemainingExpiryTime("invalid-token");

        assertEquals(0, remainingTime);
    }

    @Test
    void generateAccessToken_ShouldHaveCorrectExpiry() {
        String token = jwtUtility.generateAccessToken("user-1", "user@example.com", "CUSTOMER");

        Claims claims = jwtUtility.extractAllClaims(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();

        assertTrue(expiration.after(now));
        long expiryTime = expiration.getTime() - now.getTime();
        // Allow some tolerance (within 5 seconds)
        assertTrue(Math.abs(expiryTime - ACCESS_TOKEN_EXPIRY) < 5000);
    }

    @Test
    void generateRefreshToken_ShouldHaveCorrectExpiry() {
        String token = jwtUtility.generateRefreshToken("user-1", "user@example.com");

        Claims claims = jwtUtility.extractAllClaims(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();

        assertTrue(expiration.after(now));
        long expiryTime = expiration.getTime() - now.getTime();
        // Allow some tolerance (within 5 seconds)
        assertTrue(Math.abs(expiryTime - REFRESH_TOKEN_EXPIRY) < 5000);
    }
}

