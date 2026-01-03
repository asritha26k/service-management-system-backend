package com.app.identity_service.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtility jwtUtility;

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return isPreflight(request)
                || isPublicPath(path)
                || isPrefixedPublicPath(path)
                || isActuator(path)
                || isSwagger(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        logger.debug("[JWT Filter] Processing request: {} {}", request.getMethod(), path);

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt == null) {
                logger.warn("[JWT Filter] No JWT token found for path: {}", path);
            } else if (!jwtUtility.validateToken(jwt)) {
                logger.warn("[JWT Filter] Invalid JWT token for path: {}", path);
            } else {
                boolean needsPasswordChange = jwtUtility.extractNeedsPasswordChange(jwt);
                boolean isAllowedPath = path.equals("/api/auth/change-password") || path.equals("/api/auth/logout");

                if (needsPasswordChange && !isAllowedPath) {
                    logger.warn("[JWT Filter] Access denied: User must change password. Path={}", path);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\": \"Password change required to access this resource\"}");
                    return;
                }

                if (isAlreadyAuthenticated()) {
                    logger.debug("[JWT Filter] Authentication already present, skipping JWT processing");
                } else {
                    logger.debug("[JWT Filter] Processing valid JWT token for path: {}", path);
                    authenticate(jwt);
                }
            }

        } catch (Exception e) {
            logger.error("[JWT Filter] Error processing JWT token for path: {}", path, e);
        }

        filterChain.doFilter(request, response);
    }

    // ================= helper methods =================

    private boolean isPreflight(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(path);
    }

    private boolean isPrefixedPublicPath(String path) {
        return path.matches(".*/api/auth/(login|register|refresh)");
    }

    private boolean isActuator(String path) {
        return path.startsWith("/actuator");
    }

    private boolean isSwagger(String path) {
        return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    private boolean isAlreadyAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    private void authenticate(String jwt) {
        String email = jwtUtility.extractEmail(jwt);
        String role = jwtUtility.extractRole(jwt);
        String userId = jwtUtility.extractUserId(jwt);

        if (role == null) {
            logger.warn(
                    "[JWT Filter] CRITICAL: Role claim is null for userId={}, email={}. This will result in ROLE_null authority!",
                    userId, email);
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "UNKNOWN"));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.singletonList(authority));

        authentication.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        logger.info("[JWT Filter] Authenticated userId={}, email={}, role={}, authority={}", userId, email, role,
                authority.getAuthority());
    }

    private void logMissingOrInvalid(String jwt, String path) {
        if (jwt == null) {
            logger.warn("[JWT Filter] No JWT token found for path: {}", path);
        } else {
            logger.warn("[JWT Filter] Invalid JWT token for path: {}", path);
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null &&
                authHeader.regionMatches(true, 0, "Bearer ", 0, 7) &&
                authHeader.length() > 7) {
            return authHeader.substring(7);
        }
        return null;
    }
}
