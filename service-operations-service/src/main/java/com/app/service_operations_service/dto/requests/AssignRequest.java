package com.app.service_operations_service.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class AssignRequest {

    @NotBlank(message = "Technician ID is required")
    @Size(min = 1, max = 50, message = "Technician ID must be between 1 and 50 characters")
    private String technicianId;
}
