package com.app.service_operations_service.client.fallback;

import com.app.service_operations_service.client.IdentityClient;
import com.app.service_operations_service.client.dto.CustomerSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Fallback implementation for IdentityClient
// Used when the circuit breaker is open or the identity service is unavailable
@Slf4j
@Component
public class IdentityClientFallback implements IdentityClient {

    @Override
    public CustomerSummary getCustomer(String id) {
        log.warn("Identity service is unavailable. Cannot fetch customer: {}", id);
        return null;
    }
}
