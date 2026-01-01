package com.app.identity_service.feign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.app.identity_service.entity.dto.LoginCredentialsRequest;
import com.app.identity_service.feign.fallback.NotificationServiceClientFallback;

//communicates with notification service
@FeignClient(name = "notification-service", fallback = NotificationServiceClientFallback.class)
public interface NotificationServiceClient {

	//send credentials to staff who newly added
	@PostMapping("/api/notifications/send-credentials")
	@CircuitBreaker(name = "notification-service", fallbackMethod = "sendCredentialsEmailFallback")
	void sendCredentialsEmail(@RequestBody LoginCredentialsRequest request);
}
