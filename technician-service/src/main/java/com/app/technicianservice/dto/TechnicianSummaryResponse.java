package com.app.technicianservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianSummaryResponse {
    private String id;
    private String name;
    private String specialization;
    private Double rating;
    private Boolean available;
    private Integer currentWorkload;
    private Integer maxWorkload;
}
