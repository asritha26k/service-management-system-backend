package com.app.technicianservice.dto;

import java.time.Instant;
import java.util.List;

import com.app.technicianservice.entity.TechnicianApplication;
import com.app.technicianservice.entity.TechnicianApplication.ApplicationStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TechnicianApplicationResponse {

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
    private String previousEmployer;
    private String workExperienceDetails;
    private Integer maxWorkload;
    private String motivation;
    private Boolean hasVehicle;
    private Boolean hasToolkit;
    private String availability;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private ApplicationStatus status;
    private Instant createdAt;
    private Instant reviewedAt;
    private String reviewedBy;
    private String rejectionReason;

    public void populateFrom(TechnicianApplication app) {
        this.id = app.getId();
        this.fullName = app.getFullName();
        this.email = app.getEmail();
        this.phone = app.getPhone();
        this.address = app.getAddress();
        this.city = app.getCity();
        this.state = app.getState();
        this.zipCode = app.getZipCode();
        this.experience = app.getExperience();
        this.specialization = app.getSpecialization();
        this.skills = app.getSkillsList();
        this.certifications = app.getCertifications();
        this.previousEmployer = app.getPreviousEmployer();
        this.workExperienceDetails = app.getWorkExperienceDetails();
        this.maxWorkload = app.getMaxWorkload();
        this.motivation = app.getMotivation();
        this.hasVehicle = app.getHasVehicle();
        this.hasToolkit = app.getHasToolkit();
        this.availability = app.getAvailability();
        this.emergencyContactName = app.getEmergencyContactName();
        this.emergencyContactPhone = app.getEmergencyContactPhone();
        this.status = app.getStatus();
        this.createdAt = app.getCreatedAt();
        this.reviewedAt = app.getReviewedAt();
        this.reviewedBy = app.getReviewedBy();
        this.rejectionReason = app.getRejectionReason();
    }
}

