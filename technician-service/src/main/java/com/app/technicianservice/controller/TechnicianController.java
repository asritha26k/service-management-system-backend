package com.app.technicianservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.technicianservice.dto.ApplicationRejectionRequest;
import com.app.technicianservice.dto.ApplicationReviewResponse;
import com.app.technicianservice.dto.ApplicationSubmissionResponse;
import com.app.technicianservice.dto.AvailabilityUpdateRequest;
import com.app.technicianservice.dto.CreateProfileRequest;
import com.app.technicianservice.dto.IdMessageResponse;
import com.app.technicianservice.dto.StatsResponse;
import com.app.technicianservice.dto.SubmitRatingRequest;
import com.app.technicianservice.dto.TechnicianApplicationRequest;
import com.app.technicianservice.dto.TechnicianProfileResponse;
import com.app.technicianservice.dto.TechnicianRatingResponse;
import com.app.technicianservice.dto.TechnicianSummaryResponse;
import com.app.technicianservice.dto.WorkloadResponse;
import com.app.technicianservice.security.RequestUser;
import com.app.technicianservice.service.TechnicianApplicationService;
import com.app.technicianservice.service.TechnicianService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {

    private final TechnicianService technicianService;
    private final TechnicianApplicationService applicationService;

    public TechnicianController(
            TechnicianService technicianService,
            TechnicianApplicationService applicationService) {
        this.technicianService = technicianService;
        this.applicationService = applicationService;
    }

    // ============ Prefix-based endpoints (must come before /{id}) ============

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    public IdMessageResponse createProfile(
            RequestUser user,
            @Valid @RequestBody CreateProfileRequest request) {
        TechnicianProfileResponse profile = technicianService.createProfile(user, request);
        return new IdMessageResponse(profile.getId(), "Technician profile created successfully");
    }

    @GetMapping("/available")
    public List<TechnicianSummaryResponse> getAvailable() {
        return technicianService.getAvailable();
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return technicianService.getStats();
    }

    @GetMapping("/me")
    public TechnicianProfileResponse getMyProfile(RequestUser user) {
        return technicianService.getByUserId(user.userId());
    }

    @GetMapping("/suggestions")
    public List<TechnicianProfileResponse> getSuggestions(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<String> skills) {
        return technicianService.findSuggestions(location, skills);
    }

    @GetMapping("/by-user/{userId}")
    public TechnicianProfileResponse getByUserId(@PathVariable("userId") String userId) {
        return technicianService.getByUserId(userId);
    }

    @GetMapping("/my/workload")
    public WorkloadResponse myWorkload(RequestUser user) {
        return technicianService.getMyWorkload(user);
    }

    @PutMapping("/my/availability")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMyAvailability(
            RequestUser user,
            @Valid @RequestBody AvailabilityUpdateRequest request) {
        technicianService.updateMyAvailability(user, request);
    }

    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationSubmissionResponse apply(
            @Valid @RequestBody TechnicianApplicationRequest request) {
        return applicationService.applyForTechnician(request);
    }

    @GetMapping("/applications/pending")
    public List<ApplicationReviewResponse> pending(RequestUser user) {
        return applicationService.getPendingApplications(user);
    }

    @PostMapping("/applications/{id}/approve")
    public IdMessageResponse approve(
            RequestUser user,
            @PathVariable("id") String id) {
        ApplicationReviewResponse response = applicationService.approveApplication(user, id);
        return new IdMessageResponse(response.getId(), "Application approved successfully");
    }

    @PostMapping("/applications/{id}/reject")
    public IdMessageResponse reject(
            RequestUser user,
            @PathVariable("id") String id,
            @Valid @RequestBody ApplicationRejectionRequest request) {
        ApplicationReviewResponse response = applicationService.rejectApplication(user, id, request.getReason());
        return new IdMessageResponse(response.getId(), "Application rejected successfully");
    }

    // ============ Rating Endpoints (MUST come before other /{id}/... patterns)
    // ============

    @PostMapping("/{technicianId}/ratings")
    @ResponseStatus(HttpStatus.CREATED)
    public IdMessageResponse submitRating(
            RequestUser user,
            @PathVariable("technicianId") String technicianId,
            @Valid @RequestBody SubmitRatingRequest request) {
        TechnicianRatingResponse rating = technicianService.submitRating(user.userId(), technicianId, request);
        return new IdMessageResponse(rating.getId(), "Rating submitted successfully");
    }

    @GetMapping("/{technicianId}/ratings")
    public List<TechnicianRatingResponse> getTechnicianRatings(
            @PathVariable("technicianId") String technicianId) {
        return technicianService.getTechnicianRatings(technicianId);
    }

    @GetMapping("/{technicianId}/ratings/my-rating")
    public TechnicianRatingResponse getMyRating(
            RequestUser user,
            @PathVariable("technicianId") String technicianId) {
        return technicianService.getCustomerRating(technicianId, user.userId());
    }

    // ============ Other /{id}/... endpoints ============

    @GetMapping("/{id}/workload")
    public WorkloadResponse workload(@PathVariable("id") String id) {
        return technicianService.getWorkload(id);
    }

    @PutMapping("/{id}/workload")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateWorkload(
            @PathVariable("id") String id,
            @RequestParam("current") Integer currentWorkload) {
        technicianService.updateWorkload(id, currentWorkload);
    }

    @PutMapping("/{id}/availability")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAvailability(
            RequestUser user,
            @PathVariable("id") String id,
            @Valid @RequestBody AvailabilityUpdateRequest request) {
        technicianService.updateAvailability(user, id, request);
    }

    @PutMapping("/{id}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRating(
            RequestUser user,
            @PathVariable("id") String id,
            @RequestBody Double rating) {
        technicianService.updateRating(user, id, rating);
    }

    // ============ Generic /{id} endpoint (MUST BE LAST to avoid matching other
    // patterns) ============

    @GetMapping("/{id}")
    public TechnicianProfileResponse getById(@PathVariable("id") String id) {
        return technicianService.getById(id);
    }
}
