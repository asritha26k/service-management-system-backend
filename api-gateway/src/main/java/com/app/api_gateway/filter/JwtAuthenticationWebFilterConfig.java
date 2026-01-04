package com.app.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;

import com.app.api_gateway.security.JwtAuthenticationManager;

import reactor.core.publisher.Mono;

@Configuration
public class JwtAuthenticationWebFilterConfig {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationWebFilterConfig.class);

    private static final String[] PUBLIC_PATHS = {
            "/actuator/**",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/technicians/apply",
            "/identity-service/api/auth/login",
            "/identity-service/api/auth/register",
            "/identity-service/api/auth/refresh",
            "/technician-service/api/technicians/apply",
            "/notification-service/api/auth/login",
            "/service-operations-service/api/auth/login",
            "/eureka-server/api/auth/login"
    };

    @Bean
    public AuthenticationWebFilter jwtAuthWebFilter(JwtAuthenticationManager authenticationManager) {

        AuthenticationWebFilter filter = new AuthenticationWebFilter(authenticationManager);

        filter.setRequiresAuthenticationMatcher(exchange -> {
            String path = exchange.getRequest().getURI().getPath();
            String method = exchange.getRequest().getMethod().toString();
            logger.debug("Checking authentication requirement for {} {}", method, path);

            // Allow GET requests to catalog endpoints without authentication
            if ("GET".equals(method) && (path.matches(".*/api/catalog(/.*)?") || path.matches(".*/[^/]*/api/catalog(/.*)?") )) {
                logger.debug("Path {} is GET catalog endpoint, skipping authentication", path);
                return ServerWebExchangeMatcher.MatchResult.notMatch();
            }

            // Check if path matches any public path
            for (String publicPath : PUBLIC_PATHS) {
                // Convert Ant-style pattern to Regex
                String pattern = publicPath
                        .replace(".", "\\.")
                        .replace("/**", "(/.*)?")
                        .replace("**", ".*")
                        .replace("*", "[^/]*");
                try {
                    if (path.matches(pattern)) {
                        logger.debug("Path {} matches public pattern {}, skipping authentication", path, pattern);
                        return ServerWebExchangeMatcher.MatchResult.notMatch();
                    }
                } catch (Exception e) {
                    logger.warn("Error checking regex pattern {}: {}", pattern, e.getMessage());
                }
            }

            logger.debug("Path {} {} requires authentication", method, path);
            return ServerWebExchangeMatcher.MatchResult.match();
        });

        filter.setServerAuthenticationConverter(
                (ServerWebExchange exchange) -> {
                    String authHeader = exchange.getRequest()
                            .getHeaders()
                            .getFirst("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        logger.debug("Found Bearer token in Authorization header");
                        return Mono.just(
                                new UsernamePasswordAuthenticationToken(null, token));
                    }
                    logger.debug("No Authorization header found");
                    // Return empty Mono for requests without Authorization header
                    return Mono.empty();
                });

        return filter;
    }
}
