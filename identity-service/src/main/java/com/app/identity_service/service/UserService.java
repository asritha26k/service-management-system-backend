package com.app.identity_service.service;

import com.app.identity_service.dto.PagedResponse;
import com.app.identity_service.dto.UserAuthResponse;
import com.app.identity_service.dto.UserDetailResponse;
import com.app.identity_service.entity.UserAuth;
import com.app.identity_service.entity.UserProfile;
import com.app.identity_service.entity.UserRole;
import com.app.identity_service.exception.ResourceNotFoundException;
import com.app.identity_service.repository.UserAuthRepository;
import com.app.identity_service.repository.UserProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserAuthRepository userAuthRepository;
    
    private final UserProfileRepository userProfileRepository;

    public UserService(
            UserAuthRepository userAuthRepository,
            UserProfileRepository userProfileRepository) {
        this.userAuthRepository = userAuthRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public List<UserAuthResponse> getUsersByRole(String role) {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            return userAuthRepository.findByRole(userRole).stream()
                    .map(this::mapToUserAuthResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    public PagedResponse<UserAuthResponse> getUsersByRolePaginated(String role, Pageable pageable) {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            Page<UserAuth> userPage = userAuthRepository.findByRole(userRole, pageable);
            
            List<UserAuthResponse> content = userPage.getContent().stream()
                    .map(this::mapToUserAuthResponse)
                    .toList();
            
            return new PagedResponse<>(
                    content,
                    userPage.getNumber(),
                    userPage.getSize(),
                    userPage.getTotalElements(),
                    userPage.getTotalPages(),
                    userPage.isLast()
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    public PagedResponse<UserDetailResponse> getUsersWithDetailsByRole(String role, Pageable pageable) {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            Page<UserAuth> userPage = userAuthRepository.findByRole(userRole, pageable);
            
            List<UserDetailResponse> content = userPage.getContent().stream()
                    .map(this::mapToUserDetailResponse)
                    .toList();
            
            return new PagedResponse<>(
                    content,
                    userPage.getNumber(),
                    userPage.getSize(),
                    userPage.getTotalElements(),
                    userPage.getTotalPages(),
                    userPage.isLast()
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    public List<UserAuthResponse> searchUsersByEmail(String emailPattern) {
        return userAuthRepository.findAll().stream()
                .filter(user ->
                        user.getEmail().toLowerCase().contains(emailPattern.toLowerCase()))
                .map(this::mapToUserAuthResponse)
                .toList();
    }

    public UserAuthResponse getUserById(String userId) {
        UserAuth user = userAuthRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToUserAuthResponse(user);
    }

    public UserAuthResponse getUserByEmail(String email) {
        UserAuth user = userAuthRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToUserAuthResponse(user);
    }

    public void deactivateUser(String userId) {
        UserAuth user = userAuthRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsActive(false);
        userAuthRepository.save(user);
    }

    public void activateUser(String userId) {
        UserAuth user = userAuthRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsActive(true);
        userAuthRepository.save(user);
    }

    private UserAuthResponse mapToUserAuthResponse(UserAuth user) {
        return new UserAuthResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().toString(),
                user.getIsActive(),
                user.getIsEmailVerified(),
                user.getForcePasswordChange()
        );
    }
    
    private UserDetailResponse mapToUserDetailResponse(UserAuth user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        
        return new UserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().toString(),
                user.getIsActive(),
                user.getIsEmailVerified(),
                user.getForcePasswordChange(),
                profile != null ? profile.getName() : null,
                profile != null ? profile.getPhone() : null,
                profile != null ? profile.getAddress() : null,
                profile != null ? profile.getCity() : null,
                profile != null ? profile.getState() : null,
                profile != null ? profile.getPincode() : null
        );
    }
}
