package com.app.service_operations_service.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptRejectRequest {

    @NotBlank(message = "Action is required")
    @jakarta.validation.constraints.Pattern(regexp = "ACCEPT|REJECT", message = "Action must be either ACCEPT or REJECT")
    private String action; // ACCEPT or REJECT

    @jakarta.validation.constraints.Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason; // Optional reason for rejection
}
