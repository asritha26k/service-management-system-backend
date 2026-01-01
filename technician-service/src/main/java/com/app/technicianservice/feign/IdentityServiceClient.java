package com.app.technicianservice.feign;

import com.app.technicianservice.feign.dto.UserMeResponse;
import com.app.technicianservice.feign.fallback.IdentityServiceClientFallback;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.app.technicianservice.feign.dto.RegisterTechnicianRequest;
import com.app.technicianservice.feign.dto.UserAuthResponse;

@FeignClient(name = "identity-service", path="api/auth", fallback = IdentityServiceClientFallback.class)
public interface IdentityServiceClient {

    @PostMapping("/manager/register-technician")
    @CircuitBreaker(name = "identity-service", fallbackMethod = "registerTechnicianFallback")
    ResponseEntity<UserAuthResponse> registerTechnician(@RequestBody RegisterTechnicianRequest request);
    
    @GetMapping("/me")
    @CircuitBreaker(name = "identity-service", fallbackMethod = "getCurrentUserFallback")
    public ResponseEntity<UserMeResponse> getCurrentUser();
}

