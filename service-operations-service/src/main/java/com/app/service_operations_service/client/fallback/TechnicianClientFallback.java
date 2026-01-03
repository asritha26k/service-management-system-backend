package com.app.service_operations_service.client.fallback;

import com.app.service_operations_service.client.TechnicianClient;
import com.app.service_operations_service.client.dto.TechnicianProfileResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

// Fallback implementation for TechnicianClient
// Used when the circuit breaker is open or the technician service is unavailable
@Slf4j
@Component
public class TechnicianClientFallback implements TechnicianClient {

    @Override
    public TechnicianProfileResponse getTechnician(String id) {
        log.warn("Technician service is unavailable. Cannot fetch technician: {}", id);
        return null;
    }

    @Override
    public TechnicianProfileResponse getTechnicianByUserId(String userId) {
        log.warn("Technician service is unavailable. Cannot fetch technician by user ID: {}", userId);
        return null;
    }

    @Override
    public void updateRating(String id, Double rating) {
        log.warn("Technician service is unavailable. Cannot update rating for technician: {}", id);
    }

    @Override
    public void updateWorkload(String id, Integer currentWorkload) {
        log.warn("Technician service is unavailable. Cannot update workload for technician: {}", id);
    }

    @Override
    public Map<String, Object> getStats() {
        log.warn("Technician service is unavailable. Cannot fetch technician stats");
        return Collections.emptyMap();
    }

    @Override
    public List<TechnicianProfileResponse> getSuggestions(String location, List<String> skills) {
        log.warn("Technician service is unavailable. Cannot fetch suggestions");
        return Collections.emptyList();
    }
}
