package com.app.technicianservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.app.technicianservice.util.UserContext;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;

// Feign configuration to forward user identity headers to downstream services.
// 
// This ensures that when this service makes Feign calls to other services
// (e.g., identity-service, notification-service), the user context is preserved.
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor feignAuthInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                
                // Forward user identity headers from API Gateway
                String userId = request.getHeader(UserContext.HEADER_USER_ID);
                String userRole = request.getHeader(UserContext.HEADER_USER_ROLE);
                
                if (userId != null) {
                    requestTemplate.header(UserContext.HEADER_USER_ID, userId);
                }
                
                if (userRole != null) {
                    requestTemplate.header(UserContext.HEADER_USER_ROLE, userRole);
                }
                
                // Also forward Authorization header if present (for backward compatibility)
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }
            }
        };
    }
}
