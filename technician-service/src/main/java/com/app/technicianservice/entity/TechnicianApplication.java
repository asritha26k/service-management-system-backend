package com.app.technicianservice.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import com.app.technicianservice.dto.TechnicianApplicationRequest;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "technician_applications")
public class TechnicianApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String zipCode;

    @Column(nullable = false)
    private Integer experience; // in years

    @Column(nullable = false)
    private String specialization;

    @Column(columnDefinition = "JSON", nullable = false)
    private String skills; // Stored as JSON string

    @Column(length = 200)
    private String certifications;

    @Column(length = 100)
    private String previousEmployer;

    @Column(length = 1000)
    private String workExperienceDetails;

    @Column(nullable = false)
    private Integer maxWorkload;

    @Column(length = 1000)
    private String motivation;

    @Column
    private Boolean hasVehicle;

    @Column
    private Boolean hasToolkit;

    @Column(length = 500)
    private String availability;

    @Column(length = 100)
    private String emergencyContactName;

    @Column(length = 15)
    private String emergencyContactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant reviewedAt;

    @Column
    private String reviewedBy;

    @Column(length = 500)
    private String rejectionReason;

    public enum ApplicationStatus {
        PENDING, APPROVED, REJECTED
    }

    // Helper method to deserialize skills from JSON string to List
    public List<String> getSkillsList() {
        if (skills == null || skills.isEmpty() || skills.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(skills, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = ApplicationStatus.PENDING;
        }
        // Ensure skills is serialized to JSON string
        if (skills == null || skills.isEmpty()) {
            skills = "[]";
        }
    }
    
    public void populateFrom(TechnicianApplicationRequest r) {
        this.fullName = r.getFullName();
        this.email = r.getEmail();
        this.phone = r.getPhone();
        this.address = r.getAddress();
        this.city = r.getCity();
        this.state = r.getState();
        this.zipCode = r.getZipCode();
        this.experience = r.getExperience();
        this.specialization = r.getSpecialization();
        
        // Serialize skills list to JSON
        if (r.getSkills() != null && !r.getSkills().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.skills = mapper.writeValueAsString(r.getSkills());
            } catch (Exception e) {
                this.skills = "[]";
            }
        } else {
            this.skills = "[]";
        }
        
        this.certifications = r.getCertifications();
        this.previousEmployer = r.getPreviousEmployer();
        this.workExperienceDetails = r.getWorkExperienceDetails();
        this.maxWorkload = r.getMaxWorkload();
        this.motivation = r.getMotivation();
        this.hasVehicle = r.getHasVehicle();
        this.hasToolkit = r.getHasToolkit();
        this.availability = r.getAvailability();
        this.emergencyContactName = r.getEmergencyContactName();
        this.emergencyContactPhone = r.getEmergencyContactPhone();
        this.status = ApplicationStatus.PENDING;
    }
}

