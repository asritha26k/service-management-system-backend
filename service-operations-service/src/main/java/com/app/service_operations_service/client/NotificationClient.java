package com.app.service_operations_service.client;

import com.app.service_operations_service.client.dto.NotificationRequest;
import com.app.service_operations_service.client.fallback.NotificationClientFallback;
import com.app.service_operations_service.config.FeignConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", path = "/api/notifications", configuration = FeignConfig.class, fallback = NotificationClientFallback.class)
public interface NotificationClient {

    @PostMapping("/send")
    @CircuitBreaker(name = "notification-service", fallbackMethod = "sendNotificationFallback")
    void sendNotification(@RequestBody NotificationRequest request);

}
