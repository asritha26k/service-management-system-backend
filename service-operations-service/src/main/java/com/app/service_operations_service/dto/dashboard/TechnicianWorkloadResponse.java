package com.app.service_operations_service.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TechnicianWorkloadResponse {

    private Long totalTechnicians;
    private Long availableTechnicians;
    private Double averageWorkloadRatio;
}
