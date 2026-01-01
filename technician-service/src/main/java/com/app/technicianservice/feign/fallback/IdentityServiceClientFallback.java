package com.app.technicianservice.feign.fallback;

import com.app.technicianservice.feign.IdentityServiceClient;
import com.app.technicianservice.feign.dto.RegisterTechnicianRequest;
import com.app.technicianservice.feign.dto.UserAuthResponse;
import com.app.technicianservice.feign.dto.UserMeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

// Fallback implementation for IdentityServiceClient
// Used when the circuit breaker is open or the identity service is unavailable
@Slf4j
@Component
public class IdentityServiceClientFallback implements IdentityServiceClient {

    @Override
    public ResponseEntity<UserAuthResponse> registerTechnician(RegisterTechnicianRequest request) {
        log.warn("Identity service is unavailable. Cannot register technician: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(null);
    }

    @Override
    public ResponseEntity<UserMeResponse> getCurrentUser() {
        log.warn("Identity service is unavailable. Cannot fetch current user");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(null);
    }
}
