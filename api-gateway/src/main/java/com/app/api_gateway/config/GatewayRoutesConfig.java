package com.app.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    private static final String API_DOCS = "/v3/api-docs";

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("identity-service-api-docs", r -> r
                        .path("/identity-service" + API_DOCS + "/**")
                        .filters(f -> f.rewritePath("/identity-service" + API_DOCS, API_DOCS))
                        .uri("lb://IDENTITY-SERVICE"))
                .route("technician-service-api-docs", r -> r
                        .path("/technician-service" + API_DOCS + "/**")
                        .filters(f -> f.rewritePath("/technician-service" + API_DOCS, API_DOCS))
                        .uri("lb://TECHNICIAN-SERVICE"))
                .route("service-operations-service-api-docs", r -> r
                        .path("/service-operations-service" + API_DOCS + "/**")
                        .filters(f -> f.rewritePath("/service-operations-service" + API_DOCS, API_DOCS))
                        .uri("lb://SERVICE-OPERATIONS-SERVICE"))
                .route("notification-service-api-docs", r -> r
                        .path("/notification-service" + API_DOCS + "/**")
                        .filters(f -> f.rewritePath("/notification-service" + API_DOCS, API_DOCS))
                        .uri("lb://NOTIFICATION-SERVICE"))
                .build();
    }
}
