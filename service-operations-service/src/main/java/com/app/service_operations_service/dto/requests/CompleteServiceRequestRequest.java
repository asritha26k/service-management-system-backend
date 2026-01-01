package com.app.service_operations_service.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CompleteServiceRequestRequest {

    @NotBlank(message = "Completion notes are required")
    @Size(min = 10, max = 1000, message = "Completion notes must be between 10 and 1000 characters")
    private String completionNotes;
}
