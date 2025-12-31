package com.app.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//configuration for explicit routing, for swagger docs to see endpoints seperately
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Identity Service - OpenAPI docs
                .route("identity-service-api-docs", r -> r
                        .path("/identity-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/identity-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://IDENTITY-SERVICE"))
                
                // Technician Service - OpenAPI docs
                .route("technician-service-api-docs", r -> r
                        .path("/technician-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/technician-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://TECHNICIAN-SERVICE"))
                
                // Service Operations - OpenAPI docs
                .route("service-operations-service-api-docs", r -> r
                        .path("/service-operations-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/service-operations-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://SERVICE-OPERATIONS-SERVICE"))
                
                // Notification Service - OpenAPI docs
                .route("notification-service-api-docs", r -> r
                        .path("/notification-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/notification-service/v3/api-docs", "/v3/api-docs"))
                        .uri("lb://NOTIFICATION-SERVICE"))
                
                .build();
    }
}
