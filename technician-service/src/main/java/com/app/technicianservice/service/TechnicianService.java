package com.app.technicianservice.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.technicianservice.dto.AvailabilityUpdateRequest;
import com.app.technicianservice.dto.CreateProfileRequest;
import com.app.technicianservice.dto.StatsResponse;
import com.app.technicianservice.dto.SubmitRatingRequest;
import com.app.technicianservice.dto.TechnicianProfileResponse;
import com.app.technicianservice.dto.TechnicianRatingResponse;
import com.app.technicianservice.dto.TechnicianSummaryResponse;
import com.app.technicianservice.dto.WorkloadResponse;
import com.app.technicianservice.entity.TechnicianProfile;
import com.app.technicianservice.entity.TechnicianRating;
import com.app.technicianservice.exception.BadRequestException;
import com.app.technicianservice.exception.NotFoundException;
import com.app.technicianservice.feign.IdentityServiceClient;
import com.app.technicianservice.repository.TechnicianProfileRepository;
import com.app.technicianservice.repository.TechnicianRatingRepository;
import com.app.technicianservice.security.RequestUser;
import com.app.technicianservice.util.UserContext;

@Service
@Transactional
public class TechnicianService {

    private final TechnicianProfileRepository repository;
    private final TechnicianRatingRepository ratingRepository;
    private final IdentityServiceClient identityServiceClient;

    public TechnicianService(TechnicianProfileRepository repository, TechnicianRatingRepository ratingRepository,
            IdentityServiceClient identityServiceClient) {
        this.repository = repository;
        this.ratingRepository = ratingRepository;
        this.identityServiceClient = identityServiceClient;
    }

    public List<TechnicianProfileResponse> findSuggestions(String location, List<String> skills) {
        List<TechnicianProfile> availableTechs = repository.findByIsAvailableTrue();

        return availableTechs.stream()
                .filter(tech -> {
                    boolean locationMatch = location == null || location.isBlank() ||
                            (tech.getLocation() != null
                                    && tech.getLocation().toLowerCase().contains(location.toLowerCase()));

                    boolean skillMatch = skills == null || skills.isEmpty() ||
                            tech.getSkills().stream()
                                    .anyMatch(s -> skills.stream().anyMatch(reqS -> reqS.equalsIgnoreCase(s)));

                    return locationMatch && skillMatch;
                })
                .sorted((t1, t2) -> {
                    // Sort by workload (asc), then rating (desc)
                    int workloadCompare = Integer.compare(
                            t1.getCurrentWorkload() != null ? t1.getCurrentWorkload() : 0,
                            t2.getCurrentWorkload() != null ? t2.getCurrentWorkload() : 0);
                    if (workloadCompare != 0)
                        return workloadCompare;

                    Double r1 = t1.getRating() != null ? t1.getRating() : 0.0;
                    Double r2 = t2.getRating() != null ? t2.getRating() : 0.0;
                    return r2.compareTo(r1);
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
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
        r.setAverageRating(
                all.stream().map(TechnicianProfile::getRating)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .average().orElse(0));

        // Calculate average workload ratio: (currentWorkload / maxWorkload) * 100
        double averageWorkloadRatio = all.stream()
                .filter(p -> p.getMaxWorkload() != null && p.getMaxWorkload() > 0)
                .mapToDouble(p -> ((double) p.getCurrentWorkload() / p.getMaxWorkload()) * 100.0)
                .average()
                .orElse(0.0);
        r.setAverageWorkloadRatio(averageWorkloadRatio);

        return r;
    }

    public void updateRating(RequestUser user, String id, Double rating) {
        UserContext.requireAuthenticated(user.userId());
        TechnicianProfile p = fetch(id);
        p.setRating(rating);
        repository.save(p);
    }

    // ============ Rating System ============

    // Submit a rating for a technician
    // @param customerId ID of the customer submitting the rating
    // @param technicianId ID of the technician being rated (profile ID or user ID)
    // @param request Rating request with rating (1-5) and optional comment
    // @return The saved rating
    public TechnicianRatingResponse submitRating(String customerId, String technicianId, SubmitRatingRequest request) {
        // Validate that a technician cannot rate themselves
        if (customerId.equals(technicianId)) {
            throw new BadRequestException("Technicians cannot rate themselves");
        }

        // Fetch technician profile - try by profile ID first, then by user ID
        TechnicianProfile technician = repository.findById(technicianId)
                .orElseGet(() -> repository.findByUserId(technicianId)
                        .orElseThrow(() -> new NotFoundException("Technician not found with ID: " + technicianId)));

        // Check if customer already rated this technician
        var existingRating = ratingRepository.findByTechnicianIdAndCustomerId(technician.getId(), customerId);
        TechnicianRating rating;

        if (existingRating.isPresent()) {
            // Update existing rating
            rating = existingRating.get();
            rating.setRating(request.getRating());
            rating.setComment(request.getComment());
        } else {
            // Create new rating
            rating = new TechnicianRating(technician.getId(), customerId, request.getRating(), request.getComment());
        }

        rating = ratingRepository.save(rating);

        // Update technician's average rating
        updateTechnicianAverageRating(technician.getId());

        return new TechnicianRatingResponse(rating);
    }

    // Get all ratings for a technician
    // @param technicianId ID of the technician (profile ID or user ID)
    // @return List of ratings
    public List<TechnicianRatingResponse> getTechnicianRatings(String technicianId) {
        // Fetch technician profile - try by profile ID first, then by user ID
        TechnicianProfile technician = repository.findById(technicianId)
                .orElseGet(() -> repository.findByUserId(technicianId)
                        .orElseThrow(() -> new NotFoundException("Technician not found with ID: " + technicianId)));

        return ratingRepository.findByTechnicianId(technician.getId())
                .stream()
                .map(TechnicianRatingResponse::new)
                .toList();
    }

    // Get a specific rating by a customer for a technician
    // @param technicianId ID of the technician (profile ID or user ID)
    // @param customerId ID of the customer
    // @return The rating if exists
    public TechnicianRatingResponse getCustomerRating(String technicianId, String customerId) {
        // Validate that a technician cannot get their own rating
        if (customerId.equals(technicianId)) {
            throw new BadRequestException("Technicians cannot view their own ratings");
        }

        // Fetch technician profile - try by profile ID first, then by user ID
        TechnicianProfile technician = repository.findById(technicianId)
                .orElseGet(() -> repository.findByUserId(technicianId)
                        .orElseThrow(() -> new NotFoundException("Technician not found with ID: " + technicianId)));

        return ratingRepository.findByTechnicianIdAndCustomerId(technician.getId(), customerId)
                .map(TechnicianRatingResponse::new)
                .orElseThrow(() -> new NotFoundException("Rating not found"));
    }

    // Update average rating for a technician based on all submitted ratings
    private void updateTechnicianAverageRating(String technicianId) {
        List<TechnicianRating> ratings = ratingRepository.findByTechnicianId(technicianId);

        if (ratings.isEmpty()) {
            return;
        }

        Double averageRating = ratings.stream()
                .mapToDouble(r -> r.getRating())
                .average()
                .orElse(0.0);

        TechnicianProfile technician = fetch(technicianId);
        technician.setRating(averageRating);
        repository.save(technician);
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
                p.getRating(),
                p.getAvailable(),
                p.getCurrentWorkload(),
                p.getMaxWorkload());
    }
}
