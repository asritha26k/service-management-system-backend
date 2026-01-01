package com.app.identity_service.service;

import com.app.identity_service.dto.UserAuthResponse;
import com.app.identity_service.entity.UserAuth;
import com.app.identity_service.entity.UserRole;
import com.app.identity_service.exception.ResourceNotFoundException;
import com.app.identity_service.repository.UserAuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserAuthRepository userAuthRepository;

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
}
