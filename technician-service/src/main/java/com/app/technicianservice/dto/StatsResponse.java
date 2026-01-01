package com.app.technicianservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StatsResponse {
    private long totalTechnicians;
    private long availableTechnicians;
    private double averageRating;
    private double averageWorkloadRatio;
}
