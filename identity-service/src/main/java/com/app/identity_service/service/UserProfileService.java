package com.app.identity_service.service;

import com.app.identity_service.dto.UpdateUserProfileRequest;
import com.app.identity_service.dto.UserProfileResponse;
import com.app.identity_service.entity.UserProfile;
import com.app.identity_service.exception.DuplicateProfileException;
import com.app.identity_service.exception.ResourceNotFoundException;
import com.app.identity_service.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserProfileService {

    private static final String RESOURCE_USER_PROFILE = "UserProfile";

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfileResponse createProfile(
            String userId,
            UpdateUserProfileRequest request
    ) {
        if (userProfileRepository.existsByUserId(userId)) {
            throw new DuplicateProfileException(userId);
        }

        UserProfile profile = new UserProfile(userId, request.getName());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setPincode(request.getPincode());

        return mapToUserProfileResponse(
                userProfileRepository.save(profile)
        );
    }

    public UserProfileResponse getProfileByUserId(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                RESOURCE_USER_PROFILE, "userId", userId));

        return mapToUserProfileResponse(profile);
    }

    public UserProfileResponse getProfileById(String profileId) {
        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                RESOURCE_USER_PROFILE, "id", profileId));

        return mapToUserProfileResponse(profile);
    }

    public UserProfileResponse updateProfile(
            String userId,
            UpdateUserProfileRequest request
    ) {
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

        return mapToUserProfileResponse(
                userProfileRepository.save(profile)
        );
    }

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
                .build();
    }
}
