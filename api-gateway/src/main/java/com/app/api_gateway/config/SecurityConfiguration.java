package com.app.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            AuthenticationWebFilter jwtAuthWebFilter,
            CorsConfigurationSource corsConfigurationSource
    ) {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .addFilterBefore(jwtAuthWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchange -> exchange
                        //cors pre flight
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // public endpoints
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

                        // identity-service
                        .pathMatchers("/api/auth/me")
                        .authenticated()

                        .pathMatchers("/api/auth/admin/**")
                        .hasRole("ADMIN")

                        .pathMatchers("/api/users/role/**", "/api/users/search")
                        .hasRole("ADMIN")

                        .pathMatchers("/api/users/profile/**")
                        .hasAnyRole("ADMIN", "MANAGER", "TECHNICIAN", "CUSTOMER")

                        .pathMatchers(HttpMethod.GET, "/api/users/**")
                        .permitAll()

                        // service-catalog
                        .pathMatchers(HttpMethod.GET, "/api/catalog/categories", "/api/catalog/services")
                        .permitAll()

                        .pathMatchers("/api/catalog/categories/**", "/api/catalog/services/**")
                        .hasAnyRole("ADMIN", "MANAGER", "CUSTOMER")

                        // service-request non prefixed
                        .pathMatchers(HttpMethod.POST, "/api/service-requests")
                        .hasRole("CUSTOMER")

                        .pathMatchers(HttpMethod.GET, "/api/service-requests")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers(HttpMethod.GET, "/api/service-requests/status/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers("/api/service-requests/customer/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/api/service-requests/my-requests/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/api/service-requests/customer/*/with-technician")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/api/service-requests/technician/my-requests")
                        .hasRole("TECHNICIAN")

                        .pathMatchers("/api/service-requests/*/complete")
                        .hasRole("TECHNICIAN")

                        .pathMatchers("/api/service-requests/*/assign")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers("/api/service-requests/*/status")
                        .hasAnyRole("ADMIN", "MANAGER", "TECHNICIAN")

                        .pathMatchers("/api/service-requests/stats")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // service-requests prefixed
                        .pathMatchers(HttpMethod.POST, "/service-operations-service/api/service-requests")
                        .hasRole("CUSTOMER")

                        .pathMatchers(HttpMethod.GET, "/service-operations-service/api/service-requests")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers(HttpMethod.GET, "/service-operations-service/api/service-requests/status/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers("/service-operations-service/api/service-requests/customer/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/service-operations-service/api/service-requests/my-requests/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/service-operations-service/api/service-requests/customer/*/with-technician")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/service-operations-service/api/service-requests/technician/my-requests")
                        .hasRole("TECHNICIAN")

                        .pathMatchers("/service-operations-service/api/service-requests/*/complete")
                        .hasRole("TECHNICIAN")

                        .pathMatchers("/service-operations-service/api/service-requests/*/assign")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers("/service-operations-service/api/service-requests/*/status")
                        .hasAnyRole("ADMIN", "MANAGER", "TECHNICIAN")

                        .pathMatchers("/service-operations-service/api/service-requests/stats")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // billing requests
                        .pathMatchers("/api/billing/invoices/customer/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/api/billing/invoices/request/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/api/billing/invoices/*/pay")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/api/billing/invoices/**")
                        .hasAnyRole("ADMIN", "MANAGER", "CUSTOMER")

                        .pathMatchers("/api/billing/reports/**")
                        .hasRole("ADMIN")

                        .pathMatchers("/service-operations-service/api/billing/invoices/customer/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/service-operations-service/api/billing/invoices/request/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/service-operations-service/api/billing/invoices/*/pay")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/service-operations-service/api/billing/invoices/**")
                        .hasAnyRole("ADMIN", "MANAGER", "CUSTOMER")

                        .pathMatchers("/service-operations-service/api/billing/reports/**")
                        .hasRole("ADMIN")

                        // ratings
                        .pathMatchers(HttpMethod.POST, "/api/ratings")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/api/ratings/technician/**")
                        .permitAll()

                        .pathMatchers(HttpMethod.POST, "/service-operations-service/api/ratings")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/service-operations-service/api/ratings/technician/**")
                        .permitAll()

                        // technician service
                        .pathMatchers("/api/technicians/profile")
                        .hasRole("TECHNICIAN")

                        .pathMatchers("/api/technicians/by-user/**")
                        .hasAnyRole("ADMIN", "MANAGER", "TECHNICIAN")

                        .pathMatchers("/api/technicians/applications/pending")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers("/api/technicians/applications/*/approve")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers("/api/technicians/applications/*/reject")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers("/api/technicians/available")
                        .hasAnyRole("ADMIN", "MANAGER", "CUSTOMER")

                        .pathMatchers("/api/technicians/stats")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers("/api/technicians/*/rating")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers("/api/technicians/**")
                        .hasAnyRole("ADMIN", "MANAGER", "TECHNICIAN")

                        .pathMatchers("/technician-service/api/technicians/*/rating")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .pathMatchers("/technician-service/api/technicians/by-user/**")
                        .hasAnyRole("ADMIN", "MANAGER", "TECHNICIAN")

                        .pathMatchers("/technician-service/api/technicians/**")
                        .hasAnyRole("ADMIN", "MANAGER", "TECHNICIAN")

                        // notifications
                        .pathMatchers("/api/notifications/send-credentials")
                        .hasRole("ADMIN")

                        .pathMatchers("/api/notifications/user/**")
                        .hasAnyRole("ADMIN", "MANAGER", "TECHNICIAN", "CUSTOMER")

                        .pathMatchers("/api/notifications/**")
                        .authenticated()

                        .pathMatchers("/notification-service/api/notifications/send-credentials")
                        .hasRole("ADMIN")

                        .pathMatchers("/notification-service/api/notifications/user/**")
                        .hasAnyRole("ADMIN", "MANAGER", "TECHNICIAN", "CUSTOMER")

                        .pathMatchers("/notification-service/api/notifications/**")
                        .authenticated()

                        //fall back secure way to authenticate
                        .anyExchange().authenticated()
                );

        return http.build();
    }
}
