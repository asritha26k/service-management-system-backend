package com.app.service_operations_service.client.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TechnicianProfileResponse {

    private String id;
    private String userId;
    private String email;
    private String phone;
    private String specialization;
    private Integer experience;
    private Double rating;
    private List<String> skills;
    private String location;
    private Boolean available;
    private Integer currentWorkload;
    private Integer maxWorkload;
    private Instant createdAt;
}
