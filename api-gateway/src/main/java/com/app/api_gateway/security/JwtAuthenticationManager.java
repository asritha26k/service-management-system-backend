package com.app.api_gateway.security;

import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtUtility jwtUtility;

    public JwtAuthenticationManager(JwtUtility jwtUtility) {
        this.jwtUtility = jwtUtility;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {

        if (authentication.getCredentials() == null) {
            return Mono.error(new BadCredentialsException("Missing JWT"));
        }

        String token = authentication.getCredentials().toString();

        if (!jwtUtility.validateToken(token)) {
            return Mono.error(new BadCredentialsException("Invalid JWT"));
        }

        String userId = jwtUtility.extractUserId(token);
        String role = jwtUtility.extractRole(token);
        String email = jwtUtility.extractEmail(token);
        boolean needsPasswordChange = jwtUtility.extractNeedsPasswordChange(token);

        JwtUserPrincipal principal = new JwtUserPrincipal(userId, role, email, needsPasswordChange);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        return Mono.just(auth);
    }
}
