package com.app.service_operations_service.client;

import com.app.service_operations_service.client.dto.CustomerSummary;
import com.app.service_operations_service.client.fallback.IdentityClientFallback;
import com.app.service_operations_service.config.FeignConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service", path = "/api", configuration = FeignConfig.class, fallback = IdentityClientFallback.class)
public interface IdentityClient {

    @GetMapping("/users/{id}")
    @CircuitBreaker(name = "identity-service", fallbackMethod = "getCustomerFallback")
    CustomerSummary getCustomer(@PathVariable("id") String id);


}
