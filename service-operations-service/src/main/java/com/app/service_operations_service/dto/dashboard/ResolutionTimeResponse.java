package com.app.service_operations_service.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResolutionTimeResponse {
    private Double averageResolutionTimeHours;
    private Double averageResolutionTimeDays;

    public ResolutionTimeResponse(Double averageResolutionTimeHours) {
        this.averageResolutionTimeHours = averageResolutionTimeHours;
        this.averageResolutionTimeDays = averageResolutionTimeHours != null ? averageResolutionTimeHours / 24.0 : null;
    }
}
