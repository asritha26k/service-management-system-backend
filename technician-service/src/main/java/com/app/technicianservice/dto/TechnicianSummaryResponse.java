package com.app.technicianservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianSummaryResponse {
    private String id;
    private String userId;
    private String name;
    private String specialization;
    private List<String> skills;
    private String location;
    private Boolean available;
    private Integer currentWorkload;
    private Integer maxWorkload;
}
