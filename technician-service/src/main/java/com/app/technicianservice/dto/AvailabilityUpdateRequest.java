package com.app.technicianservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AvailabilityUpdateRequest {

    @NotNull
    private Boolean available;
}

