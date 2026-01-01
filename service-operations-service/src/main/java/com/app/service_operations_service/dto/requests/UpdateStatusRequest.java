package com.app.service_operations_service.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "REQUESTED|ASSIGNED|ACCEPTED|IN_PROGRESS|COMPLETED|CANCELLED", 
             message = "Status must be one of: REQUESTED, ASSIGNED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED")
    private String status;
}
