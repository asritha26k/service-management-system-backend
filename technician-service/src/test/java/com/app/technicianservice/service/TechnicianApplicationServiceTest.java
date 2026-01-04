package com.app.technicianservice.service;

import com.app.technicianservice.dto.*;
import com.app.technicianservice.entity.TechnicianApplication;
import com.app.technicianservice.exception.BadRequestException;
import com.app.technicianservice.exception.ConflictException;
import com.app.technicianservice.exception.NotFoundException;
import com.app.technicianservice.feign.IdentityServiceClient;
import com.app.technicianservice.feign.NotificationServiceClient;
import com.app.technicianservice.feign.dto.RegisterTechnicianRequest;
import com.app.technicianservice.feign.dto.UserAuthResponse;
import com.app.technicianservice.repository.TechnicianApplicationRepository;
import com.app.technicianservice.security.RequestUser;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnicianApplicationServiceTest {

    @Mock
    private TechnicianApplicationRepository repository;

    @Mock
    private IdentityServiceClient identityClient;

    @Mock
    private NotificationServiceClient notificationClient;

    @Mock
    private TechnicianService technicianService;

    @InjectMocks
    private TechnicianApplicationService applicationService;

    private RequestUser adminUser;
    private TechnicianApplicationRequest applicationRequest;
    private TechnicianApplication application;

    @BeforeEach
    void setUp() {
        adminUser = new RequestUser("admin-1", "ADMIN");

        applicationRequest = new TechnicianApplicationRequest();
        applicationRequest.setFullName("John Doe");
        applicationRequest.setEmail("john@example.com");
        applicationRequest.setPhone("1234567890");
        applicationRequest.setAddress("123 Main St");
        applicationRequest.setCity("New York");
        applicationRequest.setState("NY");
        applicationRequest.setZipCode("10001");
        applicationRequest.setExperience(5);
        applicationRequest.setSpecialization("Plumbing");
        applicationRequest.setSkills(Arrays.asList("Pipe Installation", "Leak Repair"));
        applicationRequest.setMaxWorkload(10);

        application = new TechnicianApplication();
        application.setId("app-1");
        application.setFullName("John Doe");
        application.setEmail("john@example.com");
        application.setPhone("1234567890");
        application.setStatus(TechnicianApplication.ApplicationStatus.PENDING);
        application.setCreatedAt(Instant.now());
    }

    @Test
    void applyForTechnician_ShouldCreateApplication() {
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(repository.save(any(TechnicianApplication.class))).thenReturn(application);

        ApplicationSubmissionResponse response = applicationService.applyForTechnician(applicationRequest);

        assertNotNull(response);
        assertEquals("app-1", response.getId());
        verify(repository, times(1)).findByEmail("john@example.com");
        verify(repository, times(1)).save(any(TechnicianApplication.class));
    }

    @Test
    void applyForTechnician_ShouldThrowBadRequest_WhenDuplicateApplication() {
        TechnicianApplication existingApp = new TechnicianApplication();
        existingApp.setStatus(TechnicianApplication.ApplicationStatus.PENDING);

        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(existingApp));

        assertThrows(BadRequestException.class, () -> applicationService.applyForTechnician(applicationRequest));
    }

    @Test
    void applyForTechnician_ShouldThrowBadRequest_WhenReapplyingWithin30Days() {
        TechnicianApplication rejectedApp = new TechnicianApplication();
        rejectedApp.setStatus(TechnicianApplication.ApplicationStatus.REJECTED);
        rejectedApp.setReviewedAt(Instant.now().minusSeconds(10 * 24 * 60 * 60)); // 10 days ago

        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(rejectedApp));

        assertThrows(BadRequestException.class, () -> applicationService.applyForTechnician(applicationRequest));
    }

    @Test
    void applyForTechnician_ShouldAllowReapply_After30Days() {
        TechnicianApplication rejectedApp = new TechnicianApplication();
        rejectedApp.setStatus(TechnicianApplication.ApplicationStatus.REJECTED);
        rejectedApp.setReviewedAt(Instant.now().minusSeconds(35L * 24 * 60 * 60)); // 35 days ago

        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(rejectedApp));
        when(repository.save(any(TechnicianApplication.class))).thenReturn(application);

        ApplicationSubmissionResponse response = applicationService.applyForTechnician(applicationRequest);

        assertNotNull(response);
        verify(repository, times(1)).save(any(TechnicianApplication.class));
    }

    @Test
    void applyForTechnician_ShouldThrowBadRequest_WhenExperienceLessThan1() {
        applicationRequest.setExperience(0);

        assertThrows(BadRequestException.class, () -> applicationService.applyForTechnician(applicationRequest));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = { 0, 21 })
    void applyForTechnician_ShouldThrowBadRequest_WhenMaxWorkloadInvalid(int maxWorkload) {
        applicationRequest.setMaxWorkload(maxWorkload);

        assertThrows(BadRequestException.class, () -> applicationService.applyForTechnician(applicationRequest));
    }

    @Test
    void getPendingApplications_ShouldReturnPendingApplications() {
        TechnicianApplication app1 = new TechnicianApplication();
        app1.setId("app-1");
        app1.setStatus(TechnicianApplication.ApplicationStatus.PENDING);

        TechnicianApplication app2 = new TechnicianApplication();
        app2.setId("app-2");
        app2.setStatus(TechnicianApplication.ApplicationStatus.PENDING);

        when(repository.findByStatus(TechnicianApplication.ApplicationStatus.PENDING))
                .thenReturn(Arrays.asList(app1, app2));

        List<ApplicationReviewResponse> responses = applicationService.getPendingApplications(adminUser);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(repository, times(1)).findByStatus(TechnicianApplication.ApplicationStatus.PENDING);
    }

    @Test
    void approveApplication_ShouldApproveApplication() {
        UserAuthResponse userAuthResponse = new UserAuthResponse();
        userAuthResponse.setId("tech-user-1");
        ResponseEntity<UserAuthResponse> registerResponse = ResponseEntity.ok(userAuthResponse);

        TechnicianProfileResponse profileResponse = new TechnicianProfileResponse();
        profileResponse.setId("profile-1");

        when(repository.findById("app-1")).thenReturn(Optional.of(application));
        when(identityClient.registerTechnician(any(RegisterTechnicianRequest.class)))
                .thenReturn(registerResponse);
        when(technicianService.createProfile(any(RequestUser.class), any(CreateProfileRequest.class)))
                .thenReturn(profileResponse);
        when(notificationClient.sendCredentialsEmail(any()))
                .thenReturn(ResponseEntity.accepted().build());
        when(repository.save(any(TechnicianApplication.class))).thenReturn(application);

        ApplicationReviewResponse response = applicationService.approveApplication(adminUser, "app-1");

        assertNotNull(response);
        assertEquals(TechnicianApplication.ApplicationStatus.APPROVED, application.getStatus());
        assertNotNull(application.getReviewedAt());
        assertEquals("admin-1", application.getReviewedBy());
        verify(identityClient, times(1)).registerTechnician(any(RegisterTechnicianRequest.class));
        verify(technicianService, times(1)).createProfile(any(RequestUser.class), any(CreateProfileRequest.class));
        verify(notificationClient, times(1)).sendCredentialsEmail(any());
    }

    @Test
    void approveApplication_ShouldThrowConflictException_WhenEmailAlreadyRegistered() {
        when(repository.findById("app-1")).thenReturn(Optional.of(application));
        when(identityClient.registerTechnician(any(RegisterTechnicianRequest.class)))
                .thenThrow(FeignException.Conflict.class);

        assertThrows(ConflictException.class, () -> applicationService.approveApplication(adminUser, "app-1"));
    }

    @Test
    void approveApplication_ShouldThrowBadRequest_WhenApplicationNotPending() {
        application.setStatus(TechnicianApplication.ApplicationStatus.APPROVED);
        when(repository.findById("app-1")).thenReturn(Optional.of(application));

        assertThrows(BadRequestException.class, () -> applicationService.approveApplication(adminUser, "app-1"));
    }

    @Test
    void approveApplication_ShouldThrowNotFoundException_WhenApplicationNotFound() {
        when(repository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> applicationService.approveApplication(adminUser, "invalid-id"));
    }

    @Test
    void rejectApplication_ShouldRejectApplication() {
        String rejectionReason = "Insufficient experience for this role";

        when(repository.findById("app-1")).thenReturn(Optional.of(application));
        when(repository.save(any(TechnicianApplication.class))).thenReturn(application);

        ApplicationReviewResponse response = applicationService.rejectApplication(
                adminUser, "app-1", rejectionReason);

        assertNotNull(response);
        assertEquals(TechnicianApplication.ApplicationStatus.REJECTED, application.getStatus());
        assertNotNull(application.getReviewedAt());
        assertEquals("admin-1", application.getReviewedBy());
        assertEquals(rejectionReason, application.getRejectionReason());
        verify(repository, times(1)).save(any(TechnicianApplication.class));
    }

    @Test
    void rejectApplication_ShouldThrowBadRequest_WhenReasonIsNull() {
        assertThrows(BadRequestException.class, () -> applicationService.rejectApplication(adminUser, "app-1", null));
    }

    @Test
    void rejectApplication_ShouldThrowBadRequest_WhenReasonIsBlank() {
        assertThrows(BadRequestException.class, () -> applicationService.rejectApplication(adminUser, "app-1", "   "));
    }

    @Test
    void rejectApplication_ShouldThrowBadRequest_WhenReasonTooShort() {
        assertThrows(BadRequestException.class,
                () -> applicationService.rejectApplication(adminUser, "app-1", "Short"));
    }

    @Test
    void rejectApplication_ShouldThrowBadRequest_WhenApplicationNotPending() {
        application.setStatus(TechnicianApplication.ApplicationStatus.APPROVED);
        when(repository.findById("app-1")).thenReturn(Optional.of(application));

        assertThrows(BadRequestException.class,
                () -> applicationService.rejectApplication(adminUser, "app-1", "Valid rejection reason here"));
    }

    @Test
    void rejectApplication_ShouldThrowNotFoundException_WhenApplicationNotFound() {
        when(repository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> applicationService.rejectApplication(adminUser, "invalid-id", "Valid rejection reason here"));
    }
}
