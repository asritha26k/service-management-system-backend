package com.app.service_operations_service.dto.requests;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class RescheduleServiceRequest {

    @NotNull(message = "Preferred date is required")
    private Instant preferredDate;
}
