package com.app.service_operations_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TechnicianSummary {
    private String id;
    private String userId;
    private String email;
    private String phone;
    private String specialization;
    private Integer experience;
    private Double rating;
    private Boolean available;

    public boolean isActive() {
        return available != null && available;
    }
}