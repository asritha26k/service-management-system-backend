package com.app.api_gateway.security;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationManager.class);

    private final JwtUtility jwtUtility;

    public JwtAuthenticationManager(JwtUtility jwtUtility) {
        this.jwtUtility = jwtUtility;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        
        logger.debug("Authenticating with JWT token");
        
        // Handle null or missing credentials
        if (authentication.getCredentials() == null) {
            logger.warn("Missing JWT token credentials");
            return Mono.error(new BadCredentialsException("Missing JWT token"));
        }

        String token = authentication.getCredentials().toString();
        
        if (token == null || token.isEmpty()) {
            logger.warn("JWT token is empty");
            return Mono.error(new BadCredentialsException("Invalid JWT token"));
        }

        if (!jwtUtility.validateToken(token)) {
            logger.warn("JWT token validation failed");
            return Mono.error(new BadCredentialsException("Invalid JWT"));
        }

        String role = jwtUtility.extractRole(token);
        String email = jwtUtility.extractEmail(token);
        
        logger.debug("Successfully authenticated user: {} with role: {}", email, role);

        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + role));

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, token, authorities);

        return Mono.just(auth);
    }
}

