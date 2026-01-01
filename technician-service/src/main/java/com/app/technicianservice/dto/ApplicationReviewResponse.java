package com.app.technicianservice.dto;

import java.time.Instant;
import java.util.List;

import com.app.technicianservice.entity.TechnicianApplication.ApplicationStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

// Application review response - what admins see when reviewing applications
@Data
@NoArgsConstructor
public class ApplicationReviewResponse {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private Integer experience;
    private String specialization;
    private List<String> skills;
    private String certifications;
    private Integer maxWorkload;
    private Boolean hasVehicle;
    private Boolean hasToolkit;
    private ApplicationStatus status;
    private Instant createdAt;
    private Instant reviewedAt;
    private String reviewedBy;
    private String rejectionReason;
}
