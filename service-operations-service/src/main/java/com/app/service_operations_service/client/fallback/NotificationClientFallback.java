package com.app.service_operations_service.client.fallback;

import com.app.service_operations_service.client.NotificationClient;
import com.app.service_operations_service.client.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Fallback implementation for NotificationClient
// Used when the circuit breaker is open or the notification service is unavailable
@Slf4j
@Component
public class NotificationClientFallback implements NotificationClient {

    @Override
    public void sendNotification(NotificationRequest request) {
        log.warn("Notification service is unavailable. Cannot send notification to: {}", request.getRecipientEmail());
    }
}
