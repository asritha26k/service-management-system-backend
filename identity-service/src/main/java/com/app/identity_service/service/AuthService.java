package com.app.identity_service.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.identity_service.dto.ChangePasswordRequest;
import com.app.identity_service.dto.LoginRequest;
import com.app.identity_service.dto.LoginResponse;
import com.app.identity_service.dto.MessageResponse;
import com.app.identity_service.dto.RefreshTokenRequest;
import com.app.identity_service.dto.RegisterCustomerRequest;
import com.app.identity_service.dto.RegisterManagerRequest;
import com.app.identity_service.dto.RegisterTechnicianRequest;
import com.app.identity_service.dto.TokenResponse;
import com.app.identity_service.dto.UpdateUserProfileRequest;
import com.app.identity_service.dto.UserAuthResponse;
import com.app.identity_service.dto.UserMeResponse;
import com.app.identity_service.entity.RefreshToken;
import com.app.identity_service.entity.UserAuth;
import com.app.identity_service.entity.UserRole;
import com.app.identity_service.exception.DuplicateResourceException;
import com.app.identity_service.exception.InvalidCredentialsException;
import com.app.identity_service.exception.InvalidTokenException;
import com.app.identity_service.exception.ResourceNotFoundException;
import com.app.identity_service.feign.NotificationServiceClient;
import com.app.identity_service.entity.dto.LoginCredentialsRequest;
import com.app.identity_service.repository.RefreshTokenRepository;
import com.app.identity_service.repository.UserAuthRepository;
import com.app.identity_service.security.JwtUtility;

// Authentication Service
// Handles user registration, login, and token management
@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private static final String EMAIL_ALREADY_REGISTERED_MSG = "Email already registered: ";

    private static final String FAILED_CREDENTIALS_EMAIL_MSG = "Failed to send credentials email: ";

    @Autowired
    private UserAuthRepository userAuthRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserProfileService userProfileService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtility jwtUtility;
    @Autowired
    private NotificationServiceClient notificationServiceClient;

    // ================= register customer =================

    public UserAuthResponse registerCustomer(RegisterCustomerRequest request) {
        if (userAuthRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    EMAIL_ALREADY_REGISTERED_MSG + request.getEmail());
        }

        UserAuth user = new UserAuth(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                UserRole.CUSTOMER);
        user.setIsEmailVerified(true);
        user.setForcePasswordChange(false);

        UserAuth savedUser = userAuthRepository.save(user);

        UpdateUserProfileRequest profileRequest = new UpdateUserProfileRequest();
        profileRequest.setName(request.getName());
        profileRequest.setPhone(request.getPhone());
        profileRequest.setAddress(request.getAddress());
        profileRequest.setCity(request.getCity());
        profileRequest.setState(request.getState());
        profileRequest.setPincode(request.getPincode());

        userProfileService.createProfile(
                savedUser.getId(),
                profileRequest);

        return mapToUserAuthResponse(savedUser);
    }

    // ================= register manager =================

    public UserAuthResponse registerManager(RegisterManagerRequest request, String adminId) {
        UserRole role = UserRole.MANAGER;

        if (userAuthRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    EMAIL_ALREADY_REGISTERED_MSG + request.getEmail());
        }

        String temporaryPassword = generateTemporaryPassword();

        UserAuth user = new UserAuth(
                request.getEmail(),
                passwordEncoder.encode(temporaryPassword),
                role);
        user.setCreatedBy(adminId);
        user.setForcePasswordChange(true);
        user.setIsEmailVerified(true);

        UserAuth savedUser = userAuthRepository.save(user);

        UpdateUserProfileRequest profileRequest = new UpdateUserProfileRequest();
        profileRequest.setName(request.getName());
        profileRequest.setPhone(request.getPhone());
        profileRequest.setDepartment(request.getDepartment());

        userProfileService.createProfile(
                savedUser.getId(),
                profileRequest,
                request.getDepartment());

        sendCredentialsEmailSafely(request.getEmail(), temporaryPassword, role);

        return mapToUserAuthResponse(savedUser);
    }

    // ================= register technician =================

    public UserAuthResponse registerTechnician(RegisterTechnicianRequest request) {
        return registerTechnicianInternal(request, null);
    }

    public UserAuthResponse registerTechnicianByManager(
            RegisterTechnicianRequest request, String managerId) {
        return registerTechnicianInternal(request, managerId);
    }

    private UserAuthResponse registerTechnicianInternal(
            RegisterTechnicianRequest request, String createdBy) {

        UserRole role = UserRole.TECHNICIAN;

        if (userAuthRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    EMAIL_ALREADY_REGISTERED_MSG + request.getEmail());
        }

        String temporaryPassword = generateTemporaryPassword();

        UserAuth user = new UserAuth(
                request.getEmail(),
                passwordEncoder.encode(temporaryPassword),
                role);
        user.setCreatedBy(createdBy);
        user.setForcePasswordChange(true);
        user.setIsEmailVerified(true);
        user.setIsActive(true);

        UserAuth savedUser = userAuthRepository.save(user);

        UpdateUserProfileRequest profileRequest = new UpdateUserProfileRequest();
        profileRequest.setName(request.getName());
        profileRequest.setPhone(request.getPhone());

        userProfileService.createProfile(
                savedUser.getId(),
                profileRequest);

        sendCredentialsEmailSafely(request.getEmail(), temporaryPassword, role);

        return mapToUserAuthResponse(savedUser);
    }

    // ================= login =================

    public LoginResponse login(LoginRequest request) {
        UserAuth user = userAuthRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("User account is inactive");
        }

        boolean passwordMatches;
        try {
            passwordMatches = passwordEncoder.matches(
                    request.getPassword(), user.getPassword());
        } catch (Exception e) {
            logger.error("Password match error", e);
            throw new InvalidCredentialsException("Error during authentication");
        }

        if (!passwordMatches) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtUtility.generateAccessToken(user.getId(), user.getEmail(), user.getRole().toString(),
                user.getForcePasswordChange());
        String refreshToken = generateAndSaveRefreshToken(user.getId(), user.getEmail());

        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().toString(),
                accessToken,
                refreshToken);
        response.setForcePasswordChange(user.getForcePasswordChange());

        return response;
    }

    // ================= helpers =================

    private void sendCredentialsEmailSafely(
            String email, String password, UserRole role) {
        try {
            notificationServiceClient.sendCredentialsEmail(
                    new LoginCredentialsRequest(email, password, role.name()));
        } catch (Exception e) {
            logger.error(FAILED_CREDENTIALS_EMAIL_MSG + e.getMessage(), e);
        }
    }

    private String generateAndSaveRefreshToken(String userId, String email) {
        String token = jwtUtility.generateRefreshToken(userId, email);
        RefreshToken refreshToken = new RefreshToken(token, userId, LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    // ================= token refresh, logout, and password management
    // =================

    public TokenResponse refreshAccessToken(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .filter(rt -> rt.getExpiryDate().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token"));

        io.jsonwebtoken.Claims claims = jwtUtility.extractAllClaims(request.getRefreshToken());
        String userId = claims.get("userId", String.class);
        String email = claims.getSubject();

        UserAuth user = userAuthRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String newAccessToken = jwtUtility.generateAccessToken(userId, email, user.getRole().toString(),
                user.getForcePasswordChange());
        return new TokenResponse(newAccessToken, request.getRefreshToken(), null);
    }

    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    public MessageResponse changePassword(String userId, ChangePasswordRequest request) {
        UserAuth user = userAuthRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidCredentialsException("New passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setForcePasswordChange(false);
        userAuthRepository.save(user);

        return new MessageResponse("Password changed successfully");
    }

    public UserMeResponse getCurrentUser(String userId) {
        UserAuth user = userAuthRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Note: UserProfileService handles profile fetching
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().toString())
                .build();
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

    private UserAuthResponse mapToUserAuthResponse(UserAuth user) {
        return new UserAuthResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().toString(),
                user.getIsActive(),
                user.getIsEmailVerified(),
                user.getForcePasswordChange());
    }
}
