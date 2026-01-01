package com.app.technicianservice.dto;

import java.time.Instant;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TechnicianProfileResponse {

    private String id;
    private String userId;
    private String email;
    private String name;
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

    public void populateFrom(com.app.technicianservice.entity.TechnicianProfile profile) {
        this.id = profile.getId();
        this.userId = profile.getUserId();
        this.email = profile.getEmail();
        this.name = profile.getName();
        this.phone = profile.getPhone();
        this.specialization = profile.getSpecialization();
        this.experience = profile.getExperience();
        this.rating = profile.getRating();
        this.skills = profile.getSkills();
        this.location = profile.getLocation();
        this.available = profile.getAvailable();
        this.currentWorkload = profile.getCurrentWorkload();
        this.maxWorkload = profile.getMaxWorkload();
        this.createdAt = profile.getCreatedAt();
    }
}
