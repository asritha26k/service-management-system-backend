package com.app.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_TECHNICIAN = "TECHNICIAN";
    private static final String ROLE_CUSTOMER = "CUSTOMER";

    private static final String USER_PROFILE = "/api/users/profile";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            AuthenticationWebFilter jwtAuthWebFilter,
            CorsConfigurationSource corsConfigurationSource
    ) {

        http
                .csrf(CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .addFilterBefore(jwtAuthWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .pathMatchers(
                                "/actuator/**",
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/technicians/apply",
                                "/identity-service/api/auth/login",
                                "/identity-service/api/auth/register",
                                "/identity-service/api/auth/refresh",
                                "/technician-service/api/technicians/apply",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/*/swagger-ui/**",
                                "/*/v3/api-docs/**",
                                "/*/swagger-ui.html",
                                "/*/api-docs/**"
                        ).permitAll()

                        .pathMatchers("/api/auth/me").authenticated()
                        .pathMatchers("/api/auth/admin/**").hasRole(ROLE_ADMIN)
                        .pathMatchers("/api/users/role/**", "/api/users/search").hasRole(ROLE_ADMIN)

                        .pathMatchers(HttpMethod.POST, USER_PROFILE).authenticated()
                        .pathMatchers(HttpMethod.GET, USER_PROFILE).authenticated()
                        .pathMatchers(HttpMethod.PUT, USER_PROFILE).authenticated()

                        .pathMatchers(USER_PROFILE + "/**")
                        .hasAnyRole(ROLE_ADMIN, ROLE_MANAGER, ROLE_TECHNICIAN, ROLE_CUSTOMER)

                        .pathMatchers(HttpMethod.GET, "/api/users/**").permitAll()

                        .pathMatchers(HttpMethod.GET, "/api/catalog/categories", "/api/catalog/services").permitAll()
                        .pathMatchers("/api/catalog/categories/**", "/api/catalog/services/**")
                        .hasAnyRole(ROLE_ADMIN, ROLE_MANAGER, ROLE_CUSTOMER)

                        .pathMatchers(HttpMethod.POST, "/api/service-requests").hasRole(ROLE_CUSTOMER)
                        .pathMatchers(HttpMethod.GET, "/api/service-requests").hasAnyRole(ROLE_ADMIN, ROLE_MANAGER)
                        .pathMatchers(HttpMethod.GET, "/api/service-requests/status/**")
                        .hasAnyRole(ROLE_ADMIN, ROLE_MANAGER)

                        .pathMatchers("/api/service-requests/customer/**").hasRole(ROLE_CUSTOMER)
                        .pathMatchers("/api/service-requests/my-requests/**").hasRole(ROLE_CUSTOMER)
                        .pathMatchers("/api/service-requests/customer/*/with-technician").hasRole(ROLE_CUSTOMER)
                        .pathMatchers("/api/service-requests/technician/my-requests").hasRole(ROLE_TECHNICIAN)
                        .pathMatchers("/api/service-requests/*/complete").hasRole(ROLE_TECHNICIAN)
                        .pathMatchers("/api/service-requests/*/assign")
                        .hasAnyRole(ROLE_ADMIN, ROLE_MANAGER)
                        .pathMatchers("/api/service-requests/*/status")
                        .hasAnyRole(ROLE_ADMIN, ROLE_MANAGER, ROLE_TECHNICIAN)
                        .pathMatchers("/api/service-requests/stats")
                        .hasAnyRole(ROLE_ADMIN, ROLE_MANAGER)

                        .pathMatchers("/api/ratings").hasRole(ROLE_CUSTOMER)
                        .pathMatchers("/api/ratings/technician/**").permitAll()

                        .pathMatchers("/api/technicians/profile").hasRole(ROLE_TECHNICIAN)
                        .pathMatchers("/api/technicians/me").hasRole(ROLE_TECHNICIAN)
                        .pathMatchers("/api/technicians/my/availability").hasRole(ROLE_TECHNICIAN)
                        .pathMatchers("/api/technicians/my/workload").hasRole(ROLE_TECHNICIAN)
                        .pathMatchers("/api/technicians/by-user/**")
                        .hasAnyRole(ROLE_ADMIN, ROLE_MANAGER, ROLE_TECHNICIAN)
                        .pathMatchers("/api/technicians/applications/**")
                        .hasAnyRole(ROLE_ADMIN, ROLE_MANAGER)
                        .pathMatchers("/api/technicians/available")
                        .hasAnyRole(ROLE_ADMIN, ROLE_MANAGER, ROLE_CUSTOMER)
                        .pathMatchers("/api/technicians/**")
                        .hasAnyRole(ROLE_ADMIN, ROLE_MANAGER, ROLE_TECHNICIAN)

                        .pathMatchers("/api/notifications/send-credentials").hasRole(ROLE_ADMIN)
                        .pathMatchers("/api/notifications/user/**")
                        .hasAnyRole(ROLE_ADMIN, ROLE_MANAGER, ROLE_TECHNICIAN, ROLE_CUSTOMER)
                        .pathMatchers("/api/notifications/**").authenticated()

                        .anyExchange().authenticated()
                );

        return http.build();
    }
}
