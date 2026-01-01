package com.app.technicianservice.feign.fallback;

import com.app.technicianservice.feign.NotificationServiceClient;
import com.app.technicianservice.feign.dto.CredentialsEmailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

// Fallback implementation for NotificationServiceClient
// Used when the circuit breaker is open or the notification service is unavailable
@Slf4j
@Component
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public ResponseEntity<Void> sendCredentialsEmail(CredentialsEmailRequest request) {
        log.warn("Notification service is unavailable. Cannot send credentials email to: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }
}
