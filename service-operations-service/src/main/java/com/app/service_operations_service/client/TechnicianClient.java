package com.app.service_operations_service.client;

import java.util.Map;

import com.app.service_operations_service.client.fallback.TechnicianClientFallback;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.service_operations_service.client.dto.TechnicianProfileResponse;

@FeignClient(name = "technician-service", path = "/api/technicians", fallback = TechnicianClientFallback.class)
public interface TechnicianClient {

    @GetMapping("/{id}")
    @CircuitBreaker(name = "technician-service", fallbackMethod = "getTechnicianFallback")
    TechnicianProfileResponse getTechnician(@PathVariable("id") String id);

    @GetMapping("/by-user/{userId}")
    @CircuitBreaker(name = "technician-service", fallbackMethod = "getTechnicianByUserIdFallback")
    TechnicianProfileResponse getTechnicianByUserId(@PathVariable("userId") String userId);

    @PutMapping("/{id}/workload")
    @CircuitBreaker(name = "technician-service", fallbackMethod = "updateWorkloadFallback")
    void updateWorkload(@PathVariable("id") String id, @RequestParam("current") Integer currentWorkload);

    @GetMapping("/stats")
    @CircuitBreaker(name = "technician-service", fallbackMethod = "getStatsFallback")
    Map<String, Object> getStats();

}
