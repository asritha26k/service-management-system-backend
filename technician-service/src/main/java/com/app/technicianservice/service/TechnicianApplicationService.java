package com.app.technicianservice.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.technicianservice.dto.ApplicationReviewResponse;
import com.app.technicianservice.dto.ApplicationSubmissionResponse;
import com.app.technicianservice.dto.CreateProfileRequest;
import com.app.technicianservice.dto.TechnicianApplicationRequest;
import com.app.technicianservice.entity.TechnicianApplication;
import com.app.technicianservice.entity.TechnicianApplication.ApplicationStatus;
import com.app.technicianservice.exception.BadRequestException;
import com.app.technicianservice.exception.ConflictException;
import com.app.technicianservice.exception.NotFoundException;
import com.app.technicianservice.feign.IdentityServiceClient;
import com.app.technicianservice.feign.NotificationServiceClient;
import com.app.technicianservice.feign.dto.CredentialsEmailRequest;
import com.app.technicianservice.feign.dto.RegisterTechnicianRequest;
import com.app.technicianservice.feign.dto.UserAuthResponse;

import feign.FeignException;
import com.app.technicianservice.repository.TechnicianApplicationRepository;
import com.app.technicianservice.security.RequestUser;
import com.app.technicianservice.util.UserContext;

@Service
@Transactional
public class TechnicianApplicationService {

    private final TechnicianApplicationRepository repository;
    private final IdentityServiceClient identityClient;
    private final NotificationServiceClient notificationClient;
    private final TechnicianService technicianService;

    public TechnicianApplicationService(
            TechnicianApplicationRepository repository,
            IdentityServiceClient identityClient,
            NotificationServiceClient notificationClient,
            TechnicianService technicianService
    ) {
        this.repository = repository;
        this.identityClient = identityClient;
        this.notificationClient = notificationClient;
        this.technicianService = technicianService;
    }

    public ApplicationSubmissionResponse applyForTechnician(TechnicianApplicationRequest request) {
        // Business Rule: Check for duplicate applications
        repository.findByEmail(request.getEmail()).ifPresent(app -> {
            if (app.getStatus() != ApplicationStatus.REJECTED) {
                throw new BadRequestException("Application already exists for this email");
            }
            // Business Rule: Cannot reapply within 30 days of rejection
            if (app.getReviewedAt() != null && 
                app.getReviewedAt().isAfter(Instant.now().minusSeconds(30L * 24 * 60 * 60))) {
                throw new BadRequestException("Cannot reapply within 30 days of rejection");
            }
        });
        
        // Business Rule: Minimum experience requirement
        if (request.getExperience() < 1) {
            throw new BadRequestException("Minimum 1 year of experience required");
        }
        
        // Business Rule: Maximum workload must be reasonable (1-20)
        if (request.getMaxWorkload() < 1 || request.getMaxWorkload() > 20) {
            throw new BadRequestException("Maximum workload must be between 1 and 20");
        }

        TechnicianApplication app = new TechnicianApplication();
        app.populateFrom(request);
        return toSubmissionResponse(repository.save(app));
    }

    public List<ApplicationReviewResponse> getPendingApplications(RequestUser user) {
        UserContext.requireAuthenticated(user.userId());
        UserContext.requireRole(user.role(), UserContext.Role.ADMIN, UserContext.Role.MANAGER);

        return repository.findByStatus(ApplicationStatus.PENDING)
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    public ApplicationReviewResponse approveApplication(RequestUser user, String id) {
        UserContext.requireAuthenticated(user.userId());
        UserContext.requireRole(user.role(), UserContext.Role.ADMIN, UserContext.Role.MANAGER);

        TechnicianApplication app = getPending(id);

        String tempPassword = UUID.randomUUID().toString().substring(0, 12);

        RegisterTechnicianRequest registerRequest =
                new RegisterTechnicianRequest(app.getEmail(), app.getFullName(), app.getPhone());

        ResponseEntity<UserAuthResponse> registerResponse;
        try {
            registerResponse = identityClient.registerTechnician(registerRequest);
        } catch (FeignException.Conflict e) {
            throw new ConflictException(
                "Cannot approve application: Email " + app.getEmail() + 
                " is already registered. The technician may have been registered previously."
            );
        } catch (FeignException e) {
            throw new BadRequestException(
                "Failed to register technician in identity service: " + e.getMessage()
            );
        }

        // Extract userId from registration response
        String technicianUserId = registerResponse.getBody().getId();

        // Create technician profile from application data
        CreateProfileRequest profileRequest = new CreateProfileRequest();
        profileRequest.setEmail(app.getEmail());
        profileRequest.setName(app.getFullName());
        profileRequest.setPhone(app.getPhone());
        profileRequest.setSkills(app.getSkillsList());
        profileRequest.setSpecialization(app.getSpecialization());
        profileRequest.setExperience(app.getExperience());
        profileRequest.setLocation(app.getCity()); // Use city as location
        profileRequest.setMaxWorkload(app.getMaxWorkload());

        // Create the technician profile with the newly registered technician's user context
        RequestUser technicianUser = new RequestUser(technicianUserId, "TECHNICIAN");
        technicianService.createProfile(technicianUser, profileRequest);

        notificationClient.sendCredentialsEmail(
                new CredentialsEmailRequest(app.getEmail(), tempPassword, "TECHNICIAN")
        );

        app.setStatus(ApplicationStatus.APPROVED);
        app.setReviewedAt(Instant.now());
        app.setReviewedBy(user.userId());

        return toReviewResponse(repository.save(app));
    }

    public ApplicationReviewResponse rejectApplication(RequestUser user, String id, String rejectionReason) {
        UserContext.requireAuthenticated(user.userId());
        UserContext.requireRole(user.role(), UserContext.Role.ADMIN, UserContext.Role.MANAGER);
        
        // Business Rule: Rejection reason is required
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new BadRequestException("Rejection reason is required");
        }
        
        if (rejectionReason.trim().length() < 10) {
            throw new BadRequestException("Rejection reason must be at least 10 characters");
        }

        TechnicianApplication app = getPending(id);
        app.setStatus(ApplicationStatus.REJECTED);
        app.setReviewedAt(Instant.now());
        app.setReviewedBy(user.userId());
        app.setRejectionReason(rejectionReason.trim());

        return toReviewResponse(repository.save(app));
    }

    private TechnicianApplication getPending(String id) {
        TechnicianApplication app = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        // Business Rule: Can only approve/reject pending applications
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException(
                "Cannot process application in " + app.getStatus() + " status. Only PENDING applications can be approved or rejected."
            );
        }
        return app;
    }

    private ApplicationSubmissionResponse toSubmissionResponse(TechnicianApplication app) {
        ApplicationSubmissionResponse r = new ApplicationSubmissionResponse();
        r.setId(app.getId());
        r.setFullName(app.getFullName());
        r.setEmail(app.getEmail());
        r.setExperience(app.getExperience());
        r.setSpecialization(app.getSpecialization());
        r.setStatus(app.getStatus());
        r.setCreatedAt(app.getCreatedAt());
        return r;
    }

    private ApplicationReviewResponse toReviewResponse(TechnicianApplication app) {
        ApplicationReviewResponse r = new ApplicationReviewResponse();
        r.setId(app.getId());
        r.setFullName(app.getFullName());
        r.setEmail(app.getEmail());
        r.setPhone(app.getPhone());
        r.setAddress(app.getAddress());
        r.setCity(app.getCity());
        r.setState(app.getState());
        r.setZipCode(app.getZipCode());
        r.setExperience(app.getExperience());
        r.setSpecialization(app.getSpecialization());
        r.setSkills(app.getSkillsList());
        r.setCertifications(app.getCertifications());
        r.setMaxWorkload(app.getMaxWorkload());
        r.setHasVehicle(app.getHasVehicle());
        r.setHasToolkit(app.getHasToolkit());
        r.setStatus(app.getStatus());
        r.setCreatedAt(app.getCreatedAt());
        r.setReviewedAt(app.getReviewedAt());
        r.setReviewedBy(app.getReviewedBy());
        r.setRejectionReason(app.getRejectionReason());
        return r;
    }
}
