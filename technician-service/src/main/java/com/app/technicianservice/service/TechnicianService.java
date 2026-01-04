package com.app.technicianservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.technicianservice.dto.AvailabilityUpdateRequest;
import com.app.technicianservice.dto.CreateProfileRequest;
import com.app.technicianservice.dto.StatsResponse;
import com.app.technicianservice.dto.TechnicianProfileResponse;
import com.app.technicianservice.dto.TechnicianSummaryResponse;
import com.app.technicianservice.dto.WorkloadResponse;
import com.app.technicianservice.entity.TechnicianProfile;
import com.app.technicianservice.exception.BadRequestException;
import com.app.technicianservice.exception.NotFoundException;
import com.app.technicianservice.feign.IdentityServiceClient;
import com.app.technicianservice.repository.TechnicianProfileRepository;
import com.app.technicianservice.security.RequestUser;
import com.app.technicianservice.util.UserContext;

@Service
@Transactional
@SuppressWarnings("null")
public class TechnicianService {

    private final TechnicianProfileRepository repository;
    private final IdentityServiceClient identityServiceClient;

    public TechnicianService(TechnicianProfileRepository repository,
            IdentityServiceClient identityServiceClient) {
        this.repository = repository;
        this.identityServiceClient = identityServiceClient;
    }

    public List<TechnicianProfileResponse> findSuggestions(String location, List<String> skills) {
        List<TechnicianProfile> availableTechs = repository.findByIsAvailableTrue();

        return availableTechs.stream()
                .filter(tech -> matchesLocation(location, tech))
                .filter(tech -> matchesSkills(skills, tech))
                .sorted(this::compareByWorkload)
                .map(this::toResponse)
                .toList();
    }

    private boolean matchesLocation(String location, TechnicianProfile tech) {
        if (location == null || location.isBlank()) {
            return true;
        }
        return tech.getLocation() != null &&
                tech.getLocation().toLowerCase().contains(location.toLowerCase());
    }

    private boolean matchesSkills(List<String> skills, TechnicianProfile tech) {
        if (skills == null || skills.isEmpty()) {
            return true;
        }
        return tech.getSkills().stream()
                .anyMatch(s -> skills.stream().anyMatch(reqS -> reqS.equalsIgnoreCase(s)));
    }

    private int compareByWorkload(TechnicianProfile t1, TechnicianProfile t2) {
        int workload1 = t1.getCurrentWorkload() != null ? t1.getCurrentWorkload() : 0;
        int workload2 = t2.getCurrentWorkload() != null ? t2.getCurrentWorkload() : 0;
        return Integer.compare(workload1, workload2);
    }

    public TechnicianProfileResponse createProfile(RequestUser user, CreateProfileRequest request) {
        UserContext.requireAuthenticated(user.userId());

        var existingProfile = repository.findByUserId(user.userId());
        if (existingProfile.isPresent()) {
            // Profile already exists - return it instead of throwing error
            // This handles the case where profile was auto-created during application
            // approval
            return toResponse(existingProfile.get());
        }

        // Use email and name from the request or fetch from identity service if needed
        String email = request.getEmail();
        String name = request.getName();

        // If email is not provided in request, try to fetch from identity service
        if (email == null || email.isBlank()) {
            var response = identityServiceClient.getCurrentUser();
            if (response == null) {
                throw new BadRequestException("Unable to retrieve user information from identity service");
            }

            var userMeResponse = response.getBody();
            if (userMeResponse == null) {
                throw new BadRequestException("Invalid response from identity service");
            }

            email = userMeResponse.getEmail();
            if (email == null || email.isBlank()) {
                throw new BadRequestException("Email not found in identity service response");
            }
        }

        // Name should always be provided in the request
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required to create technician profile");
        }

        TechnicianProfile profile = new TechnicianProfile();
        profile.populateFrom(request);
        profile.setUserId(user.userId());
        profile.setEmail(email);
        profile.setName(name);

        return toResponse(repository.save(profile));
    }

    public TechnicianProfileResponse updateAvailability(
            RequestUser user,
            String id,
            AvailabilityUpdateRequest request) {
        TechnicianProfile profile = fetch(id);

        UserContext.requireOwnershipOrAdmin(
                user.userId(),
                user.role(),
                profile.getUserId());

        profile.applyAvailabilityUpdate(request);
        return toResponse(repository.save(profile));
    }

    public TechnicianProfileResponse updateMyAvailability(
            RequestUser user,
            AvailabilityUpdateRequest request) {
        UserContext.requireAuthenticated(user.userId());

        TechnicianProfile profile = repository.findByUserId(user.userId())
                .orElseThrow(() -> new NotFoundException("Technician profile not found"));

        // Business Rule: Cannot mark available if already at max workload
        if (Boolean.TRUE.equals(request.getAvailable()) && profile.getCurrentWorkload() >= profile.getMaxWorkload()) {
            throw new BadRequestException(
                    "Cannot mark available while workload is at maximum. Complete some work first.");
        }

        profile.applyAvailabilityUpdate(request);
        return toResponse(repository.save(profile));
    }

    public TechnicianProfileResponse getById(String id) {
        return toResponse(fetch(id));
    }

    public TechnicianProfileResponse getByUserId(String userId) {
        return toResponse(
                repository.findByUserId(userId)
                        .orElseThrow(() -> new NotFoundException("Not found")));
    }

    public List<TechnicianSummaryResponse> getAvailable() {
        return repository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getAvailable()))
                .filter(p -> p.getCurrentWorkload() < p.getMaxWorkload())
                .map(this::toSummaryResponse)
                .toList();
    }

    public WorkloadResponse getWorkload(String id) {
        TechnicianProfile p = fetch(id);
        return new WorkloadResponse(p.getId(), p.getAvailable(), p.getCurrentWorkload(), p.getMaxWorkload());
    }

    public WorkloadResponse getMyWorkload(RequestUser user) {
        UserContext.requireAuthenticated(user.userId());

        TechnicianProfile p = repository.findByUserId(user.userId())
                .orElseThrow(() -> new NotFoundException("Technician profile not found"));

        return new WorkloadResponse(p.getId(), p.getAvailable(), p.getCurrentWorkload(), p.getMaxWorkload());
    }

    public StatsResponse getStats() {
        List<TechnicianProfile> all = repository.findAll();

        StatsResponse r = new StatsResponse();
        r.setTotalTechnicians(all.size());
        // Available technicians: marked available AND have capacity (currentWorkload <
        // maxWorkload)
        r.setAvailableTechnicians(all.stream()
                .filter(p -> Boolean.TRUE.equals(p.getAvailable()) && p.getCurrentWorkload() < p.getMaxWorkload())
                .count());

        // Calculate average workload ratio: (currentWorkload / maxWorkload) * 100
        double averageWorkloadRatio = all.stream()
                .filter(p -> p.getMaxWorkload() != null && p.getMaxWorkload() > 0)
                .mapToDouble(p -> ((double) p.getCurrentWorkload() / p.getMaxWorkload()) * 100.0)
                .average()
                .orElse(0.0);
        r.setAverageWorkloadRatio(averageWorkloadRatio);

        return r;
    }

    public WorkloadResponse updateWorkload(String id, Integer currentWorkload) {
        TechnicianProfile p = fetch(id);

        // Business Rule: Workload cannot be negative
        if (currentWorkload < 0) {
            throw new BadRequestException("Workload cannot be negative. Current workload: " + currentWorkload);
        }

        // Business Rule: Workload cannot exceed max workload
        if (currentWorkload > p.getMaxWorkload()) {
            throw new BadRequestException(
                    "Current workload cannot exceed maximum. Current: " + currentWorkload +
                            ", Maximum: " + p.getMaxWorkload());
        }

        p.setCurrentWorkload(currentWorkload);
        TechnicianProfile updated = repository.save(p);
        return new WorkloadResponse(updated.getId(), updated.getAvailable(), updated.getCurrentWorkload(),
                updated.getMaxWorkload());
    }

    private TechnicianProfile fetch(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Technician not found"));
    }

    private TechnicianProfileResponse toResponse(TechnicianProfile p) {
        TechnicianProfileResponse r = new TechnicianProfileResponse();
        r.populateFrom(p);
        return r;
    }

    private TechnicianSummaryResponse toSummaryResponse(TechnicianProfile p) {
        return new TechnicianSummaryResponse(
                p.getId(),
                p.getName(),
                p.getSpecialization(),
                p.getAvailable(),
                p.getCurrentWorkload(),
                p.getMaxWorkload());
    }
}
