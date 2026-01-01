package com.app.service_operations_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI serviceOperationsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Service Operations API")
                        .description("API for managing service catalog, service requests, billing, and ratings")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Service Management Team")
                                .email("support@servicemanagement.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/service-operations-service")
                                .description("Via API Gateway (Recommended)"),
                        new Server()
                                .url("http://localhost:8083")
                                .description("Direct Access (Development Only)")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token authentication"))
                        .addSecuritySchemes("user-id-header", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-User-Id")
                                .description("User ID from JWT token")));
    }
}
