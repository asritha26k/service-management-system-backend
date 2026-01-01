package com.app.technicianservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkloadResponse {
    private String technicianId;
    private Boolean available;
    private Integer currentWorkload;
    private Integer maxWorkload;
    private Double utilizationPercentage;

    public WorkloadResponse(String technicianId, Boolean available, Integer currentWorkload, Integer maxWorkload) {
        this.technicianId = technicianId;
        this.available = available;
        this.currentWorkload = currentWorkload;
        this.maxWorkload = maxWorkload;
        this.utilizationPercentage = maxWorkload > 0 ? (currentWorkload * 100.0) / maxWorkload : 0.0;
    }

    public void setCurrentWorkload(Integer currentWorkload) {
        this.currentWorkload = currentWorkload;
        if (this.maxWorkload != null && this.maxWorkload > 0) {
            this.utilizationPercentage = (currentWorkload * 100.0) / this.maxWorkload;
        }
    }

    public void setMaxWorkload(Integer maxWorkload) {
        this.maxWorkload = maxWorkload;
        if (maxWorkload > 0 && this.currentWorkload != null) {
            this.utilizationPercentage = (this.currentWorkload * 100.0) / maxWorkload;
        }
    }
}

