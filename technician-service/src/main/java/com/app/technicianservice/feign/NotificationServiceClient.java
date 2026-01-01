package com.app.technicianservice.feign;

import com.app.technicianservice.feign.fallback.NotificationServiceClientFallback;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.app.technicianservice.feign.dto.CredentialsEmailRequest;

@FeignClient(name = "notification-service", path="/api/notifications", fallback = NotificationServiceClientFallback.class)
public interface NotificationServiceClient {

    @PostMapping("/send-credentials")
    @CircuitBreaker(name = "notification-service", fallbackMethod = "sendCredentialsEmailFallback")
    ResponseEntity<Void> sendCredentialsEmail(@RequestBody CredentialsEmailRequest request);
}
