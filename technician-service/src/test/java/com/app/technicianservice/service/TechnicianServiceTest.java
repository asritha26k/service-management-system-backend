package com.app.technicianservice.service;

import com.app.technicianservice.dto.*;
import com.app.technicianservice.entity.TechnicianProfile;
import com.app.technicianservice.exception.BadRequestException;
import com.app.technicianservice.exception.NotFoundException;
import com.app.technicianservice.feign.IdentityServiceClient;
import com.app.technicianservice.feign.dto.UserMeResponse;
import com.app.technicianservice.repository.TechnicianProfileRepository;
import com.app.technicianservice.security.RequestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

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

        assertThrows(BadRequestException.class, () -> technicianService.createProfile(testUser, createRequest));
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

        assertThrows(NotFoundException.class, () -> technicianService.getById("invalid-id"));
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

        assertThrows(NotFoundException.class, () -> technicianService.getByUserId("invalid-user"));
    }

    @Test
    void getAvailable_ShouldReturnOnlyAvailableTechnicians() {
        TechnicianProfile availableProfile = new TechnicianProfile();
        availableProfile.setId("profile-1");
        availableProfile.setUserId("user-1");
        availableProfile.setName("John Doe");
        availableProfile.setSpecialization("Plumbing");
        availableProfile.setSkills(Arrays.asList("Pipe Repair", "Water Heater"));
        availableProfile.setLocation("New York");
        availableProfile.setAvailable(true);
        availableProfile.setCurrentWorkload(2);
        availableProfile.setMaxWorkload(5);

        TechnicianProfile unavailableProfile = new TechnicianProfile();
        unavailableProfile.setId("profile-2");
        unavailableProfile.setUserId("user-2");
        unavailableProfile.setName("Jane Smith");
        unavailableProfile.setSpecialization("HVAC");
        unavailableProfile.setSkills(Arrays.asList("AC Repair", "Heating"));
        unavailableProfile.setLocation("Boston");
        unavailableProfile.setAvailable(false);
        unavailableProfile.setCurrentWorkload(0);
        unavailableProfile.setMaxWorkload(5);

        TechnicianProfile fullWorkloadProfile = new TechnicianProfile();
        fullWorkloadProfile.setId("profile-3");
        fullWorkloadProfile.setUserId("user-3");
        fullWorkloadProfile.setName("Bob Johnson");
        fullWorkloadProfile.setSpecialization("Electrical");
        fullWorkloadProfile.setSkills(Arrays.asList("Wiring", "Panel Installation"));
        fullWorkloadProfile.setLocation("Chicago");
        fullWorkloadProfile.setAvailable(true);
        fullWorkloadProfile.setCurrentWorkload(5);
        fullWorkloadProfile.setMaxWorkload(5);

        when(repository.findAll()).thenReturn(Arrays.asList(
                availableProfile, unavailableProfile, fullWorkloadProfile));

        List<TechnicianSummaryResponse> responses = technicianService.getAvailable();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("profile-1", responses.get(0).getId());
        assertEquals("user-1", responses.get(0).getUserId());
        assertEquals("John Doe", responses.get(0).getName());
        assertEquals(Arrays.asList("Pipe Repair", "Water Heater"), responses.get(0).getSkills());
        assertEquals("New York", responses.get(0).getLocation());
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

        TechnicianProfile profile2 = new TechnicianProfile();
        profile2.setAvailable(true);
        profile2.setCurrentWorkload(1);
        profile2.setMaxWorkload(5);

        TechnicianProfile profile3 = new TechnicianProfile();
        profile3.setAvailable(false);
        profile3.setCurrentWorkload(0);
        profile3.setMaxWorkload(5);

        when(repository.findAll()).thenReturn(Arrays.asList(profile1, profile2, profile3));

        StatsResponse stats = technicianService.getStats();

        assertNotNull(stats);
        assertEquals(3, stats.getTotalTechnicians());
        assertEquals(2, stats.getAvailableTechnicians());
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

        assertThrows(BadRequestException.class, () -> technicianService.updateMyAvailability(testUser, request));
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = { -1, 10 })
    void updateWorkload_ShouldThrowBadRequest_WhenWorkloadInvalid(int currentWorkload) {
        when(repository.findById("profile-1")).thenReturn(Optional.of(profile));

        assertThrows(BadRequestException.class, () -> technicianService.updateWorkload("profile-1", currentWorkload));
    }

    // Tests for findSuggestions method and its lambda expressions
    @Test
    void findSuggestions_ShouldReturnAllAvailableTechnicians_WhenNoFilters() {
        TechnicianProfile tech1 = new TechnicianProfile();
        tech1.setId("tech-1");
        tech1.setName("Tech One");
        tech1.setAvailable(true);
        tech1.setCurrentWorkload(1);
        tech1.setMaxWorkload(5);
        tech1.setLocation("New York");
        tech1.setSkills(Arrays.asList("Electrical", "Plumbing"));

        TechnicianProfile tech2 = new TechnicianProfile();
        tech2.setId("tech-2");
        tech2.setName("Tech Two");
        tech2.setAvailable(true);
        tech2.setCurrentWorkload(2);
        tech2.setMaxWorkload(5);
        tech2.setLocation("Boston");
        tech2.setSkills(Arrays.asList("HVAC"));

        when(repository.findByIsAvailableTrue()).thenReturn(Arrays.asList(tech1, tech2));

        List<TechnicianProfileResponse> results = technicianService.findSuggestions(null, null);

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(repository, times(1)).findByIsAvailableTrue();
    }

    @Test
    void findSuggestions_ShouldFilterByLocation() {
        TechnicianProfile tech1 = new TechnicianProfile();
        tech1.setId("tech-1");
        tech1.setName("Tech One");
        tech1.setAvailable(true);
        tech1.setCurrentWorkload(1);
        tech1.setMaxWorkload(5);
        tech1.setLocation("New York");
        tech1.setSkills(Arrays.asList("Electrical"));

        TechnicianProfile tech2 = new TechnicianProfile();
        tech2.setId("tech-2");
        tech2.setName("Tech Two");
        tech2.setAvailable(true);
        tech2.setCurrentWorkload(2);
        tech2.setMaxWorkload(5);
        tech2.setLocation("Boston");
        tech2.setSkills(Arrays.asList("HVAC"));

        when(repository.findByIsAvailableTrue()).thenReturn(Arrays.asList(tech1, tech2));

        List<TechnicianProfileResponse> results = technicianService.findSuggestions("New York", null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("tech-1", results.get(0).getId());
    }

    @Test
    void findSuggestions_ShouldFilterBySkills() {
        TechnicianProfile tech1 = new TechnicianProfile();
        tech1.setId("tech-1");
        tech1.setName("Tech One");
        tech1.setAvailable(true);
        tech1.setCurrentWorkload(1);
        tech1.setMaxWorkload(5);
        tech1.setLocation("New York");
        tech1.setSkills(Arrays.asList("Electrical", "Plumbing"));

        TechnicianProfile tech2 = new TechnicianProfile();
        tech2.setId("tech-2");
        tech2.setName("Tech Two");
        tech2.setAvailable(true);
        tech2.setCurrentWorkload(2);
        tech2.setMaxWorkload(5);
        tech2.setLocation("Boston");
        tech2.setSkills(Arrays.asList("HVAC"));

        when(repository.findByIsAvailableTrue()).thenReturn(Arrays.asList(tech1, tech2));

        List<TechnicianProfileResponse> results = technicianService.findSuggestions(null, Arrays.asList("Electrical"));

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("tech-1", results.get(0).getId());
    }

    @Test
    void findSuggestions_ShouldFilterByLocationAndSkills() {
        TechnicianProfile tech1 = new TechnicianProfile();
        tech1.setId("tech-1");
        tech1.setName("Tech One");
        tech1.setAvailable(true);
        tech1.setCurrentWorkload(1);
        tech1.setMaxWorkload(5);
        tech1.setLocation("New York");
        tech1.setSkills(Arrays.asList("Electrical", "Plumbing"));

        TechnicianProfile tech2 = new TechnicianProfile();
        tech2.setId("tech-2");
        tech2.setName("Tech Two");
        tech2.setAvailable(true);
        tech2.setCurrentWorkload(2);
        tech2.setMaxWorkload(5);
        tech2.setLocation("New York");
        tech2.setSkills(Arrays.asList("HVAC"));

        TechnicianProfile tech3 = new TechnicianProfile();
        tech3.setId("tech-3");
        tech3.setName("Tech Three");
        tech3.setAvailable(true);
        tech3.setCurrentWorkload(3);
        tech3.setMaxWorkload(5);
        tech3.setLocation("Boston");
        tech3.setSkills(Arrays.asList("Electrical"));

        when(repository.findByIsAvailableTrue()).thenReturn(Arrays.asList(tech1, tech2, tech3));

        List<TechnicianProfileResponse> results = technicianService.findSuggestions("New York",
                Arrays.asList("Electrical"));

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("tech-1", results.get(0).getId());
    }

    @Test
    void findSuggestions_ShouldSortByWorkloadAscending() {
        TechnicianProfile tech1 = new TechnicianProfile();
        tech1.setId("tech-1");
        tech1.setName("Tech One");
        tech1.setAvailable(true);
        tech1.setCurrentWorkload(4);
        tech1.setMaxWorkload(5);
        tech1.setLocation("New York");
        tech1.setSkills(Arrays.asList("Electrical"));

        TechnicianProfile tech2 = new TechnicianProfile();
        tech2.setId("tech-2");
        tech2.setName("Tech Two");
        tech2.setAvailable(true);
        tech2.setCurrentWorkload(1);
        tech2.setMaxWorkload(5);
        tech2.setLocation("New York");
        tech2.setSkills(Arrays.asList("Electrical"));

        when(repository.findByIsAvailableTrue()).thenReturn(Arrays.asList(tech1, tech2));

        List<TechnicianProfileResponse> results = technicianService.findSuggestions("New York",
                Arrays.asList("Electrical"));

        assertNotNull(results);
        assertEquals(2, results.size());
        // Should be sorted by workload ascending (lowest first)
        assertEquals("tech-2", results.get(0).getId());
        assertEquals("tech-1", results.get(1).getId());
    }

    @Test
    void findSuggestions_ShouldHandleNullWorkload() {
        TechnicianProfile tech1 = new TechnicianProfile();
        tech1.setId("tech-1");
        tech1.setName("Tech One");
        tech1.setAvailable(true);
        tech1.setCurrentWorkload(null); // Null workload
        tech1.setMaxWorkload(5);
        tech1.setLocation("New York");
        tech1.setSkills(Arrays.asList("Electrical"));

        TechnicianProfile tech2 = new TechnicianProfile();
        tech2.setId("tech-2");
        tech2.setName("Tech Two");
        tech2.setAvailable(true);
        tech2.setCurrentWorkload(1);
        tech2.setMaxWorkload(5);
        tech2.setLocation("New York");
        tech2.setSkills(Arrays.asList("Electrical"));

        when(repository.findByIsAvailableTrue()).thenReturn(Arrays.asList(tech1, tech2));

        List<TechnicianProfileResponse> results = technicianService.findSuggestions("New York", null);

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void findSuggestions_ShouldHandleBlankLocation() {
        TechnicianProfile tech1 = new TechnicianProfile();
        tech1.setId("tech-1");
        tech1.setName("Tech One");
        tech1.setAvailable(true);
        tech1.setCurrentWorkload(1);
        tech1.setMaxWorkload(5);
        tech1.setLocation("New York");
        tech1.setSkills(Arrays.asList("Electrical"));

        when(repository.findByIsAvailableTrue()).thenReturn(Arrays.asList(tech1));

        List<TechnicianProfileResponse> results = technicianService.findSuggestions("", null);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void findSuggestions_ShouldHandleCaseInsensitiveLocationMatch() {
        TechnicianProfile tech1 = new TechnicianProfile();
        tech1.setId("tech-1");
        tech1.setName("Tech One");
        tech1.setAvailable(true);
        tech1.setCurrentWorkload(1);
        tech1.setMaxWorkload(5);
        tech1.setLocation("new york");
        tech1.setSkills(Arrays.asList("Electrical"));

        when(repository.findByIsAvailableTrue()).thenReturn(Arrays.asList(tech1));

        List<TechnicianProfileResponse> results = technicianService.findSuggestions("NEW YORK", null);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void findSuggestions_ShouldHandleCaseInsensitiveSkillMatch() {
        TechnicianProfile tech1 = new TechnicianProfile();
        tech1.setId("tech-1");
        tech1.setName("Tech One");
        tech1.setAvailable(true);
        tech1.setCurrentWorkload(1);
        tech1.setMaxWorkload(5);
        tech1.setLocation("New York");
        tech1.setSkills(Arrays.asList("electrical"));

        when(repository.findByIsAvailableTrue()).thenReturn(Arrays.asList(tech1));

        List<TechnicianProfileResponse> results = technicianService.findSuggestions(null, Arrays.asList("ELECTRICAL"));

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    // Additional tests for createProfile with identity service
    @Test
    void createProfile_ShouldFetchEmailFromIdentityService_WhenEmailNotInRequest() {
        CreateProfileRequest requestWithoutEmail = new CreateProfileRequest();
        requestWithoutEmail.setName("John Doe");
        requestWithoutEmail.setPhone("1234567890");
        requestWithoutEmail.setSkills(Arrays.asList("Electrical"));
        requestWithoutEmail.setLocation("New York");
        requestWithoutEmail.setMaxWorkload(5);
        requestWithoutEmail.setEmail(null);

        UserMeResponse userResponse = new UserMeResponse();
        userResponse.setId("user-1");
        userResponse.setEmail("tech@example.com");

        when(repository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(identityServiceClient.getCurrentUser()).thenReturn(ResponseEntity.ok(userResponse));
        when(repository.save(any(TechnicianProfile.class))).thenReturn(profile);

        TechnicianProfileResponse response = technicianService.createProfile(testUser, requestWithoutEmail);

        assertNotNull(response);
        assertEquals("profile-1", response.getId());
        verify(identityServiceClient, times(1)).getCurrentUser();
        verify(repository, times(1)).save(any(TechnicianProfile.class));
    }

    @Test
    void createProfile_ShouldThrowBadRequest_WhenIdentityServiceReturnsNull() {
        CreateProfileRequest requestWithoutEmail = new CreateProfileRequest();
        requestWithoutEmail.setName("John Doe");
        requestWithoutEmail.setPhone("1234567890");
        requestWithoutEmail.setSkills(Arrays.asList("Electrical"));
        requestWithoutEmail.setLocation("New York");
        requestWithoutEmail.setMaxWorkload(5);
        requestWithoutEmail.setEmail(null);

        when(repository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(identityServiceClient.getCurrentUser()).thenReturn(null);

        assertThrows(BadRequestException.class, () -> technicianService.createProfile(testUser, requestWithoutEmail));
    }

    @Test
    void createProfile_ShouldThrowBadRequest_WhenIdentityServiceResponseBodyIsNull() {
        CreateProfileRequest requestWithoutEmail = new CreateProfileRequest();
        requestWithoutEmail.setName("John Doe");
        requestWithoutEmail.setPhone("1234567890");
        requestWithoutEmail.setSkills(Arrays.asList("Electrical"));
        requestWithoutEmail.setLocation("New York");
        requestWithoutEmail.setMaxWorkload(5);
        requestWithoutEmail.setEmail(null);

        when(repository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(identityServiceClient.getCurrentUser()).thenReturn(ResponseEntity.ok(null));

        assertThrows(BadRequestException.class, () -> technicianService.createProfile(testUser, requestWithoutEmail));
    }

    @Test
    void createProfile_ShouldThrowBadRequest_WhenIdentityServiceEmailIsBlank() {
        CreateProfileRequest requestWithoutEmail = new CreateProfileRequest();
        requestWithoutEmail.setName("John Doe");
        requestWithoutEmail.setPhone("1234567890");
        requestWithoutEmail.setSkills(Arrays.asList("Electrical"));
        requestWithoutEmail.setLocation("New York");
        requestWithoutEmail.setMaxWorkload(5);
        requestWithoutEmail.setEmail("");

        UserMeResponse userResponse = new UserMeResponse();
        userResponse.setId("user-1");
        userResponse.setEmail("");

        when(repository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(identityServiceClient.getCurrentUser()).thenReturn(ResponseEntity.ok(userResponse));

        assertThrows(BadRequestException.class, () -> technicianService.createProfile(testUser, requestWithoutEmail));
    }

    // Additional tests for getMyWorkload edge cases
    @Test
    void getMyWorkload_ShouldThrowNotFoundException_WhenProfileNotFound() {
        RequestUser newUser = new RequestUser("user-2", "TECHNICIAN");

        when(repository.findByUserId("user-2")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> technicianService.getMyWorkload(newUser));
    }

    @Test
    void getMyWorkload_ShouldReturnCorrectWorkloadData() {
        TechnicianProfile testProfile = new TechnicianProfile();
        testProfile.setId("profile-1");
        testProfile.setUserId("user-1");
        testProfile.setAvailable(true);
        testProfile.setCurrentWorkload(3);
        testProfile.setMaxWorkload(8);

        when(repository.findByUserId("user-1")).thenReturn(Optional.of(testProfile));

        WorkloadResponse response = technicianService.getMyWorkload(testUser);

        assertNotNull(response);
        assertEquals("profile-1", response.getTechnicianId());
        assertEquals(3, response.getCurrentWorkload());
        assertEquals(8, response.getMaxWorkload());
        assertEquals(true, response.getAvailable());
    }

    // Additional tests for updateMyAvailability edge cases
    @Test
    void updateMyAvailability_ShouldThrowNotFoundException_WhenProfileNotFound() {
        RequestUser newUser = new RequestUser("user-2", "TECHNICIAN");
        AvailabilityUpdateRequest request = new AvailabilityUpdateRequest();
        request.setAvailable(true);

        when(repository.findByUserId("user-2")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> technicianService.updateMyAvailability(newUser, request));
    }

    @Test
    void updateMyAvailability_ShouldAllowAvailableTrue_WhenBelowMaxWorkload() {
        AvailabilityUpdateRequest request = new AvailabilityUpdateRequest();
        request.setAvailable(true);

        TechnicianProfile testProfile = new TechnicianProfile();
        testProfile.setId("profile-1");
        testProfile.setUserId("user-1");
        testProfile.setAvailable(false);
        testProfile.setCurrentWorkload(2);
        testProfile.setMaxWorkload(5);

        when(repository.findByUserId("user-1")).thenReturn(Optional.of(testProfile));
        when(repository.save(any(TechnicianProfile.class))).thenReturn(testProfile);

        TechnicianProfileResponse response = technicianService.updateMyAvailability(testUser, request);

        assertNotNull(response);
        verify(repository, times(1)).save(any(TechnicianProfile.class));
    }

    @Test
    void updateMyAvailability_ShouldAllowAvailableFalse_WhenAtMaxWorkload() {
        AvailabilityUpdateRequest request = new AvailabilityUpdateRequest();
        request.setAvailable(false);

        TechnicianProfile testProfile = new TechnicianProfile();
        testProfile.setId("profile-1");
        testProfile.setUserId("user-1");
        testProfile.setAvailable(true);
        testProfile.setCurrentWorkload(5);
        testProfile.setMaxWorkload(5);

        when(repository.findByUserId("user-1")).thenReturn(Optional.of(testProfile));
        when(repository.save(any(TechnicianProfile.class))).thenReturn(testProfile);

        TechnicianProfileResponse response = technicianService.updateMyAvailability(testUser, request);

        assertNotNull(response);
        verify(repository, times(1)).save(any(TechnicianProfile.class));
    }

    @Test
    void updateMyAvailability_ShouldThrowBadRequest_WhenTryingAvailableTrue_AtMaxWorkloadEdgeCase() {
        AvailabilityUpdateRequest request = new AvailabilityUpdateRequest();
        request.setAvailable(true);

        TechnicianProfile testProfile = new TechnicianProfile();
        testProfile.setId("profile-1");
        testProfile.setUserId("user-1");
        testProfile.setAvailable(false);
        testProfile.setCurrentWorkload(5);
        testProfile.setMaxWorkload(5);

        when(repository.findByUserId("user-1")).thenReturn(Optional.of(testProfile));

        assertThrows(BadRequestException.class, () -> technicianService.updateMyAvailability(testUser, request));
    }
}
