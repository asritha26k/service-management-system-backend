package com.app.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.app.api_gateway.security.JwtUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import reactor.core.publisher.Mono;

@Component
public class IdentityPropagationFilter implements GlobalFilter, Ordered {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    Authentication authentication = securityContext.getAuthentication();
                    return processAuthenticated(exchange, chain, authentication);
                })
                .switchIfEmpty(Mono.defer(() -> processUnauthenticated(exchange, chain)));
    }

    private Mono<Void> processAuthenticated(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return processUnauthenticated(exchange, chain);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof JwtUserPrincipal)) {
            return processUnauthenticated(exchange, chain);
        }

        JwtUserPrincipal user = (JwtUserPrincipal) principal;

        String path = exchange.getRequest().getURI().getPath();
        if (user.isNeedsPasswordChange()) {
            // Only allow change-password and logout related paths (from identity service)
            boolean isAllowed = path.contains("/api/auth/change-password") || path.contains("/api/auth/logout");

            if (!isAllowed) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory().wrap("{\"message\": \"Password change required\"}".getBytes())));
            }
        }

        String userId = user.getUserId();
        String role = user.getRole();

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USER_ROLE);

                    if (userId != null && !userId.trim().isEmpty()) {
                        headers.add(HEADER_USER_ID, userId);
                    }

                    if (role != null && !role.trim().isEmpty()) {
                        headers.add(HEADER_USER_ROLE, role);
                    }

                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> processUnauthenticated(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USER_ROLE);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
