package com.app.technicianservice.service;

import com.app.technicianservice.dto.*;
import com.app.technicianservice.entity.TechnicianProfile;
import com.app.technicianservice.entity.TechnicianRating;
import com.app.technicianservice.exception.BadRequestException;
import com.app.technicianservice.exception.NotFoundException;
import com.app.technicianservice.feign.IdentityServiceClient;
import com.app.technicianservice.repository.TechnicianProfileRepository;
import com.app.technicianservice.repository.TechnicianRatingRepository;
import com.app.technicianservice.security.RequestUser;
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
class TechnicianServiceTest {

    @Mock
    private TechnicianProfileRepository repository;

    @Mock
    private TechnicianRatingRepository ratingRepository;

    @Mock
    private IdentityServiceClient identityServiceClient;

    @InjectMocks
    private TechnicianService technicianService;

    private RequestUser testUser;
    private TechnicianProfile profile;
    private CreateProfileRequest createRequest;

    @BeforeEach
    void setUp() {
        testUser = new RequestUser("user-1", "TECHNICIAN");

        profile = new TechnicianProfile();
        profile.setId("profile-1");
        profile.setUserId("user-1");
        profile.setEmail("tech@example.com");
        profile.setName("John Doe");
        profile.setPhone("1234567890");
        profile.setAvailable(true);
        profile.setCurrentWorkload(2);
        profile.setMaxWorkload(5);
        profile.setSkills(Arrays.asList("Plumbing", "Electrical"));
        profile.setLocation("New York");
        profile.setRating(4.5);

        createRequest = new CreateProfileRequest();
        createRequest.setEmail("tech@example.com");
        createRequest.setName("John Doe");
        createRequest.setPhone("1234567890");
        createRequest.setSkills(Arrays.asList("Plumbing", "Electrical"));
        createRequest.setLocation("New York");
        createRequest.setMaxWorkload(5);
    }

    @Test
    void createProfile_ShouldCreateNewProfile() {
        when(repository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(repository.save(any(TechnicianProfile.class))).thenReturn(profile);

        TechnicianProfileResponse response = technicianService.createProfile(testUser, createRequest);

        assertNotNull(response);
        assertEquals("profile-1", response.getId());
        assertEquals("user-1", response.getUserId());
        verify(repository, times(1)).findByUserId("user-1");
        verify(repository, times(1)).save(any(TechnicianProfile.class));
    }

    @Test
    void createProfile_ShouldReturnExistingProfile_WhenProfileExists() {
        when(repository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        TechnicianProfileResponse response = technicianService.createProfile(testUser, createRequest);

        assertNotNull(response);
        assertEquals("profile-1", response.getId());
        verify(repository, times(1)).findByUserId("user-1");
        verify(repository, never()).save(any(TechnicianProfile.class));
    }

    @Test
    void createProfile_ShouldThrowBadRequest_WhenNameIsBlank() {
        createRequest.setName("");
        when(repository.findByUserId("user-1")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> 
            technicianService.createProfile(testUser, createRequest));
    }

    @Test
    void getById_ShouldReturnProfile() {
        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));

        TechnicianProfileResponse response = technicianService.getById("profile-1");

        assertNotNull(response);
        assertEquals("profile-1", response.getId());
        verify(repository, times(1)).findById("profile-1");
    }

    @Test
    void getById_ShouldThrowNotFoundException_WhenNotFound() {
        when(repository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> 
            technicianService.getById("invalid-id"));
    }

    @Test
    void getByUserId_ShouldReturnProfile() {
        when(repository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        TechnicianProfileResponse response = technicianService.getByUserId("user-1");

        assertNotNull(response);
        assertEquals("profile-1", response.getId());
        verify(repository, times(1)).findByUserId("user-1");
    }

    @Test
    void getByUserId_ShouldThrowNotFoundException_WhenNotFound() {
        when(repository.findByUserId("invalid-user")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> 
            technicianService.getByUserId("invalid-user"));
    }

    @Test
    void getAvailable_ShouldReturnOnlyAvailableTechnicians() {
        TechnicianProfile availableProfile = new TechnicianProfile();
        availableProfile.setId("profile-1");
        availableProfile.setAvailable(true);
        availableProfile.setCurrentWorkload(2);
        availableProfile.setMaxWorkload(5);

        TechnicianProfile unavailableProfile = new TechnicianProfile();
        unavailableProfile.setId("profile-2");
        unavailableProfile.setAvailable(false);
        unavailableProfile.setCurrentWorkload(0);
        unavailableProfile.setMaxWorkload(5);

        TechnicianProfile fullWorkloadProfile = new TechnicianProfile();
        fullWorkloadProfile.setId("profile-3");
        fullWorkloadProfile.setAvailable(true);
        fullWorkloadProfile.setCurrentWorkload(5);
        fullWorkloadProfile.setMaxWorkload(5);

        when(repository.findAll()).thenReturn(Arrays.asList(
            availableProfile, unavailableProfile, fullWorkloadProfile
        ));

        List<TechnicianSummaryResponse> responses = technicianService.getAvailable();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("profile-1", responses.get(0).getId());
        verify(repository, times(1)).findAll();
    }

    @Test
    void getWorkload_ShouldReturnWorkload() {
        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));

        WorkloadResponse response = technicianService.getWorkload("profile-1");

        assertNotNull(response);
        assertEquals("profile-1", response.getTechnicianId());
        assertEquals(2, response.getCurrentWorkload());
        assertEquals(5, response.getMaxWorkload());
    }

    @Test
    void getMyWorkload_ShouldReturnWorkload() {
        when(repository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        WorkloadResponse response = technicianService.getMyWorkload(testUser);

        assertNotNull(response);
        assertEquals("profile-1", response.getTechnicianId());
        assertEquals(2, response.getCurrentWorkload());
    }

    @Test
    void getStats_ShouldCalculateStatsCorrectly() {
        TechnicianProfile profile1 = new TechnicianProfile();
        profile1.setAvailable(true);
        profile1.setCurrentWorkload(2);
        profile1.setMaxWorkload(5);
        profile1.setRating(4.5);

        TechnicianProfile profile2 = new TechnicianProfile();
        profile2.setAvailable(true);
        profile2.setCurrentWorkload(1);
        profile2.setMaxWorkload(5);
        profile2.setRating(4.0);

        TechnicianProfile profile3 = new TechnicianProfile();
        profile3.setAvailable(false);
        profile3.setCurrentWorkload(0);
        profile3.setMaxWorkload(5);
        profile3.setRating(3.5);

        when(repository.findAll()).thenReturn(Arrays.asList(profile1, profile2, profile3));

        StatsResponse stats = technicianService.getStats();

        assertNotNull(stats);
        assertEquals(3, stats.getTotalTechnicians());
        assertEquals(2, stats.getAvailableTechnicians());
        assertEquals(4.0, stats.getAverageRating(), 0.01);
        assertTrue(stats.getAverageWorkloadRatio() > 0);
    }

    @Test
    void updateAvailability_ShouldUpdateAvailability() {
        AvailabilityUpdateRequest request = new AvailabilityUpdateRequest();
        request.setAvailable(false);

        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(TechnicianProfile.class))).thenReturn(profile);

        TechnicianProfileResponse response = technicianService.updateAvailability(
            testUser, "profile-1", request);

        assertNotNull(response);
        verify(repository, times(1)).findById("profile-1");
        verify(repository, times(1)).save(any(TechnicianProfile.class));
    }

    @Test
    void updateMyAvailability_ShouldUpdateAvailability() {
        AvailabilityUpdateRequest request = new AvailabilityUpdateRequest();
        request.setAvailable(false);

        when(repository.findByUserId("user-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(TechnicianProfile.class))).thenReturn(profile);

        TechnicianProfileResponse response = technicianService.updateMyAvailability(
            testUser, request);

        assertNotNull(response);
        verify(repository, times(1)).findByUserId("user-1");
        verify(repository, times(1)).save(any(TechnicianProfile.class));
    }

    @Test
    void updateMyAvailability_ShouldThrowBadRequest_WhenAtMaxWorkload() {
        AvailabilityUpdateRequest request = new AvailabilityUpdateRequest();
        request.setAvailable(true);
        profile.setCurrentWorkload(5);
        profile.setMaxWorkload(5);

        when(repository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        assertThrows(BadRequestException.class, () -> 
            technicianService.updateMyAvailability(testUser, request));
    }

    @Test
    void updateWorkload_ShouldUpdateWorkload() {
        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(TechnicianProfile.class))).thenAnswer(invocation -> {
            TechnicianProfile p = invocation.getArgument(0);
            p.setCurrentWorkload(3);
            return p;
        });

        WorkloadResponse response = technicianService.updateWorkload("profile-1", 3);

        assertNotNull(response);
        assertEquals(3, response.getCurrentWorkload());
        verify(repository, times(1)).save(any(TechnicianProfile.class));
    }

    @Test
    void updateWorkload_ShouldThrowBadRequest_WhenNegative() {
        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));

        assertThrows(BadRequestException.class, () -> 
            technicianService.updateWorkload("profile-1", -1));
    }

    @Test
    void updateWorkload_ShouldThrowBadRequest_WhenExceedsMax() {
        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));

        assertThrows(BadRequestException.class, () -> 
            technicianService.updateWorkload("profile-1", 10));
    }

    @Test
    void submitRating_ShouldCreateNewRating() {
        SubmitRatingRequest request = new SubmitRatingRequest();
        request.setRating(5);
        request.setComment("Excellent service");

        TechnicianRating rating = new TechnicianRating("profile-1", "customer-1", 5, "Excellent service");
        rating.setId("rating-1");

        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(ratingRepository.findByTechnicianIdAndCustomerId("profile-1", "customer-1"))
            .thenReturn(Optional.empty());
        when(ratingRepository.save(any(TechnicianRating.class))).thenReturn(rating);
        when(ratingRepository.findByTechnicianId("profile-1")).thenReturn(Arrays.asList(rating));
        when(repository.save(any(TechnicianProfile.class))).thenReturn(profile);

        TechnicianRatingResponse response = technicianService.submitRating(
            "customer-1", "profile-1", request);

        assertNotNull(response);
        assertEquals("rating-1", response.getId());
        verify(ratingRepository, times(1)).save(any(TechnicianRating.class));
    }

    @Test
    void submitRating_ShouldUpdateExistingRating() {
        SubmitRatingRequest request = new SubmitRatingRequest();
        request.setRating(4);
        request.setComment("Updated comment");

        TechnicianRating existingRating = new TechnicianRating("profile-1", "customer-1", 5, "Old comment");
        existingRating.setId("rating-1");

        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(ratingRepository.findByTechnicianIdAndCustomerId("profile-1", "customer-1"))
            .thenReturn(Optional.of(existingRating));
        when(ratingRepository.save(any(TechnicianRating.class))).thenReturn(existingRating);
        when(ratingRepository.findByTechnicianId("profile-1")).thenReturn(Arrays.asList(existingRating));
        when(repository.save(any(TechnicianProfile.class))).thenReturn(profile);

        TechnicianRatingResponse response = technicianService.submitRating(
            "customer-1", "profile-1", request);

        assertNotNull(response);
        verify(ratingRepository, times(1)).save(any(TechnicianRating.class));
    }

    @Test
    void submitRating_ShouldThrowBadRequest_WhenRatingSelf() {
        SubmitRatingRequest request = new SubmitRatingRequest();
        request.setRating(5);

        assertThrows(BadRequestException.class, () -> 
            technicianService.submitRating("profile-1", "profile-1", request));
    }

    @Test
    void getTechnicianRatings_ShouldReturnRatings() {
        TechnicianRating rating1 = new TechnicianRating("profile-1", "customer-1", 5, "Great");
        rating1.setId("rating-1");
        TechnicianRating rating2 = new TechnicianRating("profile-1", "customer-2", 4, "Good");
        rating2.setId("rating-2");

        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(ratingRepository.findByTechnicianId("profile-1"))
            .thenReturn(Arrays.asList(rating1, rating2));

        List<TechnicianRatingResponse> responses = technicianService.getTechnicianRatings("profile-1");

        assertNotNull(responses);
        assertEquals(2, responses.size());
    }

    @Test
    void getCustomerRating_ShouldReturnRating() {
        TechnicianRating rating = new TechnicianRating("profile-1", "customer-1", 5, "Great");
        rating.setId("rating-1");

        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(ratingRepository.findByTechnicianIdAndCustomerId("profile-1", "customer-1"))
            .thenReturn(Optional.of(rating));

        TechnicianRatingResponse response = technicianService.getCustomerRating("profile-1", "customer-1");

        assertNotNull(response);
        assertEquals("rating-1", response.getId());
    }

    @Test
    void getCustomerRating_ShouldThrowBadRequest_WhenRatingSelf() {
        assertThrows(BadRequestException.class, () -> 
            technicianService.getCustomerRating("profile-1", "profile-1"));
    }

    @Test
    void updateRating_ShouldUpdateRating() {
        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(TechnicianProfile.class))).thenReturn(profile);

        technicianService.updateRating(testUser, "profile-1", 4.5);

        verify(repository, times(1)).save(any(TechnicianProfile.class));
    }
}

