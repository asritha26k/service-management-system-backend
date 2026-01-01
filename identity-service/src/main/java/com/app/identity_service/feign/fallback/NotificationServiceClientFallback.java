package com.app.identity_service.feign.fallback;

import com.app.identity_service.feign.NotificationServiceClient;
import com.app.identity_service.entity.dto.LoginCredentialsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Fallback implementation for NotificationServiceClient
// Used when the circuit breaker is open or the notification service is unavailable
@Slf4j
@Component
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public void sendCredentialsEmail(LoginCredentialsRequest request) {
        log.warn("Notification service is unavailable. Cannot send credentials email to: {}", request.getEmail());
    }
}
