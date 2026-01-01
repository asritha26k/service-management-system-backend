package com.app.technicianservice.dto;

import java.time.Instant;

import com.app.technicianservice.entity.TechnicianApplication.ApplicationStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

// Application submission response - what the applicant sees after submitting
@Data
@NoArgsConstructor
public class ApplicationSubmissionResponse {
    private String id;
    private String fullName;
    private String email;
    private Integer experience;
    private String specialization;
    private ApplicationStatus status;
    private Instant createdAt;
}
