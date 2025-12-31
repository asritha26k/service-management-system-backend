package com.app.api_gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.properties.SwaggerUiConfigParameters;

import java.util.ArrayList;
import java.util.List;

//configuration for swagger documentation
@Configuration
public class SwaggerConfig {

    @Autowired
    private RouteDefinitionLocator locator;

    //configure the main open ai
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Service Management System API")
                        .version("1.0.0")
                        .description("Unified API documentation for all microservices in the Service Management System")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@servicemanagement.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway - Local Environment")
                ))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearer-jwt", new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token authentication")))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearer-jwt"));
    }

    //creating grouped api for identity service
    @Bean
    public GroupedOpenApi identityServiceApi() {
        return GroupedOpenApi.builder()
                .group("identity-service")
                .pathsToMatch("/identity-service/**")
                .build();
    }

    //creating grouped api for technician service
    @Bean
    public GroupedOpenApi technicianServiceApi() {
        return GroupedOpenApi.builder()
                .group("technician-service")
                .pathsToMatch("/technician-service/**")
                .build();
    }

  //creating grouped api for service operation service
    @Bean
    public GroupedOpenApi serviceOperationsServiceApi() {
        return GroupedOpenApi.builder()
                .group("service-operations-service")
                .pathsToMatch("/service-operations-service/**")
                .build();
    }

    //creating grouped api for notification service
    @Bean
    public GroupedOpenApi notificationServiceApi() {
        return GroupedOpenApi.builder()
                .group("notification-service")
                .pathsToMatch("/notification-service/**")
                .build();
    }

    //grouped api for all the services
    @Bean
    public GroupedOpenApi allServicesApi() {
        return GroupedOpenApi.builder()
                .group("all-services")
                .pathsToMatch("/**")
                .pathsToExclude("/actuator/**")
                .build();
    }
}
