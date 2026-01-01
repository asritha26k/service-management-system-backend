package com.app.service_operations_service.dto.requests;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import lombok.Data;

@Data
public class CreateServiceRequest {

    @NotBlank(message = "Service ID is required")
    @Size(min = 1, max = 50, message = "Service ID must be between 1 and 50 characters")
    private String serviceId;

    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT", message = "Priority must be one of: LOW, MEDIUM, HIGH, URGENT")
    private String priority;

    @NotNull(message = "Preferred date is required")
    private Instant preferredDate;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 500, message = "Address must be between 5 and 500 characters")
    private String address;
}
