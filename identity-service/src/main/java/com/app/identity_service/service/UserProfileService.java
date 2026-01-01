package com.app.identity_service.service;

import com.app.identity_service.dto.UpdateUserProfileRequest;
import com.app.identity_service.dto.UserProfileResponse;
import com.app.identity_service.entity.UserProfile;
import com.app.identity_service.exception.DuplicateProfileException;
import com.app.identity_service.exception.ResourceNotFoundException;
import com.app.identity_service.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserProfileService {

    private static final String RESOURCE_USER_PROFILE = "UserProfile";

    @Autowired
    private UserProfileRepository userProfileRepository;

    // Create user profile (customer / default)
    public UserProfileResponse createProfile(String userId, UpdateUserProfileRequest request) {
        return createProfileInternal(userId, request);
    }

    // Create user profile with department (staff)
    public UserProfileResponse createProfile(
            String userId,
            UpdateUserProfileRequest request,
            String department
    ) {
        request.setDepartment(department);
        return createProfileInternal(userId, request);
    }

    private UserProfileResponse createProfileInternal(
            String userId,
            UpdateUserProfileRequest request
    ) {
        // Check if profile already exists for this user
        if (userProfileRepository.existsByUserId(userId)) {
            throw new DuplicateProfileException(userId);
        }

        UserProfile profile = new UserProfile(userId, request.getName());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setPincode(request.getPincode());
        profile.setDepartment(request.getDepartment());

        return mapToUserProfileResponse(
                userProfileRepository.save(profile)
        );
    }

    // Get user profile by user ID
    public UserProfileResponse getProfileByUserId(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                RESOURCE_USER_PROFILE, "userId", userId));

        return mapToUserProfileResponse(profile);
    }

    // Get user profile by profile ID
    public UserProfileResponse getProfileById(String profileId) {
        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                RESOURCE_USER_PROFILE, "id", profileId));

        return mapToUserProfileResponse(profile);
    }

    // Update user profile
    public UserProfileResponse updateProfile(String userId, UpdateUserProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                RESOURCE_USER_PROFILE, "userId", userId));

        profile.setName(request.getName());
        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getState() != null) profile.setState(request.getState());
        if (request.getPincode() != null) profile.setPincode(request.getPincode());
        if (request.getDepartment() != null) profile.setDepartment(request.getDepartment());

        return mapToUserProfileResponse(
                userProfileRepository.save(profile)
        );
    }

    // Map UserProfile entity to response DTO
    private UserProfileResponse mapToUserProfileResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .name(profile.getName())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .city(profile.getCity())
                .state(profile.getState())
                .pincode(profile.getPincode())
                .department(profile.getDepartment())
                .build();
    }
}
