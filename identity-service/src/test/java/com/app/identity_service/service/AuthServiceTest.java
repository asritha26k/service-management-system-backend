package com.app.identity_service.service;

import com.app.identity_service.dto.*;
import com.app.identity_service.entity.RefreshToken;
import com.app.identity_service.entity.UserAuth;
import com.app.identity_service.entity.UserRole;
import com.app.identity_service.exception.DuplicateResourceException;
import com.app.identity_service.exception.InvalidCredentialsException;
import com.app.identity_service.exception.ResourceNotFoundException;
import com.app.identity_service.feign.NotificationServiceClient;
import com.app.identity_service.repository.RefreshTokenRepository;
import com.app.identity_service.repository.UserAuthRepository;
import com.app.identity_service.security.JwtUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtility jwtUtility;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private AuthService authService;

    private RegisterCustomerRequest registerRequest;
    private UserAuth userAuth;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterCustomerRequest();
        registerRequest.setEmail("customer@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setName("John Doe");
        registerRequest.setPhone("1234567890");
        registerRequest.setAddress("123 Main St");
        registerRequest.setCity("New York");
        registerRequest.setState("NY");
        registerRequest.setPincode("10001");

        userAuth = new UserAuth();
        userAuth.setId("user-1");
        userAuth.setEmail("customer@example.com");
        userAuth.setPassword("encodedPassword");
        userAuth.setRole(UserRole.CUSTOMER);
        userAuth.setIsActive(true);
        userAuth.setIsEmailVerified(true);
        userAuth.setForcePasswordChange(false);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("customer@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void registerCustomer_ShouldCreateCustomer() {
        when(userAuthRepository.existsByEmail("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(userAuth);
        when(userProfileService.createProfile(anyString(), any(UpdateUserProfileRequest.class)))
                .thenReturn(UserProfileResponse.builder().userId("user-1").build());

        UserAuthResponse response = authService.registerCustomer(registerRequest);

        assertNotNull(response);
        assertEquals("user-1", response.getId());
        assertEquals("customer@example.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        verify(userAuthRepository, times(1)).existsByEmail("customer@example.com");
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
        verify(userProfileService, times(1)).createProfile(anyString(), any(UpdateUserProfileRequest.class));
    }

    @Test
    void registerCustomer_ShouldThrowDuplicateResourceException_WhenEmailExists() {
        when(userAuthRepository.existsByEmail("customer@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> 
            authService.registerCustomer(registerRequest));
        
        verify(userAuthRepository, never()).save(any(UserAuth.class));
    }

    @Test
    void registerManager_ShouldCreateManager() {
        RegisterManagerRequest request = new RegisterManagerRequest();
        request.setEmail("manager@example.com");
        request.setName("Manager Name");
        request.setPhone("1234567890");
        request.setDepartment("Operations");

        when(userAuthRepository.existsByEmail("manager@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(userAuth);
        when(userProfileService.createProfile(anyString(), any(UpdateUserProfileRequest.class), anyString()))
                .thenReturn(UserProfileResponse.builder().userId("user-1").build());
        doNothing().when(notificationServiceClient).sendCredentialsEmail(any());

        UserAuthResponse response = authService.registerManager(request, "admin-1");

        assertNotNull(response);
        assertEquals("user-1", response.getId());
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
        verify(notificationServiceClient, times(1)).sendCredentialsEmail(any());
    }

    @Test
    void registerTechnician_ShouldCreateTechnician() {
        RegisterTechnicianRequest request = new RegisterTechnicianRequest();
        request.setEmail("tech@example.com");
        request.setName("Tech Name");
        request.setPhone("1234567890");

        when(userAuthRepository.existsByEmail("tech@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(userAuth);
        when(userProfileService.createProfile(anyString(), any(UpdateUserProfileRequest.class)))
                .thenReturn(UserProfileResponse.builder().userId("user-1").build());
        doNothing().when(notificationServiceClient).sendCredentialsEmail(any());

        UserAuthResponse response = authService.registerTechnician(request);

        assertNotNull(response);
        assertEquals("user-1", response.getId());
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
        verify(notificationServiceClient, times(1)).sendCredentialsEmail(any());
    }

    @Test
    void login_ShouldReturnLoginResponse() {
        when(userAuthRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(userAuth));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtility.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("access-token");
        when(jwtUtility.generateRefreshToken(anyString(), anyString())).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("user-1", response.getUserId());
        assertEquals("customer@example.com", response.getEmail());
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(userAuthRepository, times(1)).findByEmail("customer@example.com");
        verify(passwordEncoder, times(1)).matches("password123", "encodedPassword");
    }

    @Test
    void login_ShouldThrowInvalidCredentialsException_WhenUserNotFound() {
        when(userAuthRepository.findByEmail("customer@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> 
            authService.login(loginRequest));
    }

    @Test
    void login_ShouldThrowInvalidCredentialsException_WhenUserInactive() {
        userAuth.setIsActive(false);
        when(userAuthRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(userAuth));

        assertThrows(InvalidCredentialsException.class, () -> 
            authService.login(loginRequest));
    }

    @Test
    void login_ShouldThrowInvalidCredentialsException_WhenPasswordMismatch() {
        when(userAuthRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(userAuth));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> 
            authService.login(loginRequest));
    }

    @Test
    void refreshAccessToken_ShouldReturnTokenResponse() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setUserId("user-1");
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(userAuthRepository.findById("user-1")).thenReturn(Optional.of(userAuth));
        when(jwtUtility.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("new-access-token");

        TokenResponse response = authService.refreshAccessToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(refreshTokenRepository, times(1)).findByToken("refresh-token");
    }

    @Test
    void refreshAccessToken_ShouldThrowInvalidCredentialsException_WhenTokenNotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");

        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> 
            authService.refreshAccessToken(request));
    }

    @Test
    void logout_ShouldDeleteRefreshTokens() {
        authService.logout("user-1");

        verify(refreshTokenRepository, times(1)).deleteByUserId("user-1");
    }

    @Test
    void changePassword_ShouldChangePassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(userAuthRepository.findById("user-1")).thenReturn(Optional.of(userAuth));
        when(passwordEncoder.matches("oldPassword123", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(userAuth);

        MessageResponse response = authService.changePassword("user-1", request);

        assertNotNull(response);
        assertEquals("Password changed successfully", response.getMessage());
        verify(userAuthRepository, times(1)).save(any(UserAuth.class));
    }

    @Test
    void changePassword_ShouldThrowInvalidCredentialsException_WhenCurrentPasswordIncorrect() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(userAuthRepository.findById("user-1")).thenReturn(Optional.of(userAuth));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> 
            authService.changePassword("user-1", request));
    }

    @Test
    void changePassword_ShouldThrowInvalidCredentialsException_WhenPasswordsDoNotMatch() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("differentPassword");

        when(userAuthRepository.findById("user-1")).thenReturn(Optional.of(userAuth));
        when(passwordEncoder.matches("oldPassword123", "encodedPassword")).thenReturn(true);

        assertThrows(InvalidCredentialsException.class, () -> 
            authService.changePassword("user-1", request));
    }

    @Test
    void getCurrentUser_ShouldReturnUserMeResponse() {
        when(userAuthRepository.findById("user-1")).thenReturn(Optional.of(userAuth));

        UserMeResponse response = authService.getCurrentUser("user-1");

        assertNotNull(response);
        assertEquals("user-1", response.getId());
        assertEquals("customer@example.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
    }

    @Test
    void getCurrentUser_ShouldThrowResourceNotFoundException_WhenUserNotFound() {
        when(userAuthRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            authService.getCurrentUser("invalid-id"));
    }
}

