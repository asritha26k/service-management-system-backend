package com.app.identity_service.service;

import com.app.identity_service.dto.UpdateUserProfileRequest;
import com.app.identity_service.dto.UserProfileResponse;
import com.app.identity_service.entity.UserProfile;
import com.app.identity_service.exception.DuplicateProfileException;
import com.app.identity_service.exception.ResourceNotFoundException;
import com.app.identity_service.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    private UpdateUserProfileRequest profileRequest;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        profileRequest = new UpdateUserProfileRequest();
        profileRequest.setName("John Doe");
        profileRequest.setPhone("1234567890");
        profileRequest.setAddress("123 Main St");
        profileRequest.setCity("New York");
        profileRequest.setState("NY");
        profileRequest.setPincode("10001");

        userProfile = new UserProfile();
        userProfile.setId("profile-1");
        userProfile.setUserId("user-1");
        userProfile.setName("John Doe");
        userProfile.setPhone("1234567890");
        userProfile.setAddress("123 Main St");
        userProfile.setCity("New York");
        userProfile.setState("NY");
        userProfile.setPincode("10001");
    }

    @Test
    void createProfile_ShouldCreateProfile() {
        when(userProfileRepository.existsByUserId("user-1")).thenReturn(false);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(userProfile);

        UserProfileResponse response = userProfileService.createProfile("user-1", profileRequest);

        assertNotNull(response);
        assertEquals("profile-1", response.getId());
        assertEquals("user-1", response.getUserId());
        assertEquals("John Doe", response.getName());
        verify(userProfileRepository, times(1)).existsByUserId("user-1");
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    void createProfile_ShouldThrowDuplicateProfileException_WhenProfileExists() {
        when(userProfileRepository.existsByUserId("user-1")).thenReturn(true);

        assertThrows(DuplicateProfileException.class, () -> 
            userProfileService.createProfile("user-1", profileRequest));
        
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void createProfile_ShouldCreateProfileWithThirdParameter() {
        when(userProfileRepository.existsByUserId("user-1")).thenReturn(false);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(userProfile);

        UserProfileResponse response = userProfileService.createProfile("user-1", profileRequest, null);

        assertNotNull(response);
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    void getProfileByUserId_ShouldReturnProfile() {
        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(userProfile));

        UserProfileResponse response = userProfileService.getProfileByUserId("user-1");

        assertNotNull(response);
        assertEquals("profile-1", response.getId());
        assertEquals("user-1", response.getUserId());
        verify(userProfileRepository, times(1)).findByUserId("user-1");
    }

    @Test
    void getProfileByUserId_ShouldThrowResourceNotFoundException_WhenNotFound() {
        when(userProfileRepository.findByUserId("invalid-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            userProfileService.getProfileByUserId("invalid-id"));
    }

    @Test
    void getProfileById_ShouldReturnProfile() {
        when(userProfileRepository.findById("profile-1")).thenReturn(Optional.of(userProfile));

        UserProfileResponse response = userProfileService.getProfileById("profile-1");

        assertNotNull(response);
        assertEquals("profile-1", response.getId());
        verify(userProfileRepository, times(1)).findById("profile-1");
    }

    @Test
    void getProfileById_ShouldThrowResourceNotFoundException_WhenNotFound() {
        when(userProfileRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            userProfileService.getProfileById("invalid-id"));
    }

    @Test
    void updateProfile_ShouldUpdateProfile() {
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest();
        updateRequest.setName("Jane Doe");
        updateRequest.setPhone("9876543210");
        updateRequest.setAddress("456 Oak Ave");

        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(userProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(userProfile);

        UserProfileResponse response = userProfileService.updateProfile("user-1", updateRequest);

        assertNotNull(response);
        assertEquals("Jane Doe", userProfile.getName());
        assertEquals("9876543210", userProfile.getPhone());
        assertEquals("456 Oak Ave", userProfile.getAddress());
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    void updateProfile_ShouldUpdateOnlyProvidedFields() {
        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest();
        updateRequest.setName("Jane Doe");
        // Phone and address are null - should not update them

        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(userProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(userProfile);

        UserProfileResponse response = userProfileService.updateProfile("user-1", updateRequest);

        assertNotNull(response);
        assertEquals("Jane Doe", userProfile.getName());
        // Original values should remain
        assertEquals("1234567890", userProfile.getPhone());
        assertEquals("123 Main St", userProfile.getAddress());
    }

    @Test
    void updateProfile_ShouldThrowResourceNotFoundException_WhenNotFound() {
        when(userProfileRepository.findByUserId("invalid-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            userProfileService.updateProfile("invalid-id", profileRequest));
    }
}

