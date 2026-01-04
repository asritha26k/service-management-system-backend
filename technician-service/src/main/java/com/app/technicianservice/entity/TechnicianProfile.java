package com.app.technicianservice.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.app.technicianservice.dto.CreateProfileRequest;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "technician_profiles")
public class TechnicianProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String phone;

    private String specialization;
    private Integer experience;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(nullable = false)
    private List<String> skills = new ArrayList<>();

    @Column(nullable = false)
    private Boolean isAvailable;

    @Column(nullable = false)
    private Integer currentWorkload;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Integer maxWorkload;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    public void populateFrom(CreateProfileRequest r) {
        this.name = r.getName();
        this.phone = r.getPhone();
        this.skills = r.getSkills() != null ? r.getSkills() : new ArrayList<>();
        this.specialization = r.getSpecialization();
        this.experience = r.getExperience();
        this.location = r.getLocation();
        this.maxWorkload = r.getMaxWorkload();
        this.isAvailable = true;
        this.currentWorkload = 0;
    }

    public void applyAvailabilityUpdate(com.app.technicianservice.dto.AvailabilityUpdateRequest request) {
        // AvailabilityUpdateRequest only contains the availability toggle
        if (request.getAvailable() != null) {
            this.isAvailable = request.getAvailable();
        }
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (isAvailable == null) isAvailable = Boolean.TRUE;
        if (currentWorkload == null) currentWorkload = 0;
        if (maxWorkload == null) maxWorkload = 5;
        if (skills == null) skills = new ArrayList<>();
    }

    public Boolean getAvailable() { 
        return isAvailable; 
    }
    
    public void setAvailable(Boolean available) { 
        isAvailable = available; 
    }
}
