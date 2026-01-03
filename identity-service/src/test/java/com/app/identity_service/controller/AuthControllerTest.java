package com.app.identity_service.controller;

import com.app.identity_service.dto.*;
import com.app.identity_service.exception.DuplicateResourceException;
import com.app.identity_service.exception.GlobalExceptionHandler;
import com.app.identity_service.exception.InvalidCredentialsException;
import com.app.identity_service.service.AuthService;
import com.app.identity_service.util.SecurityUtil;
import com.app.identity_service.security.JwtUtility;
import com.app.identity_service.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
    EurekaClientAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
    "logging.level.root=INFO",
    "logging.level.com.app.identity_service=INFO",
    "spring.application.name=identity-service-test",
    "server.port=0",
    "jwt.secret=test-secret-key-for-testing-purposes-only-must-be-at-least-256-bits-long-for-hmac-sha-256-algorithm",
    "jwt.access-token-expiry=3600000",
    "jwt.refresh-token-expiry=604800000",
    "LOG_LEVEL_ROOT=INFO",
    "LOG_LEVEL_APP=INFO",
    "SPRING_APPLICATION_NAME=identity-service-test",
    "SERVER_PORT=0",
    "JWT_SECRET=test-secret-key-for-testing-purposes-only-must-be-at-least-256-bits-long-for-hmac-sha-256-algorithm",
    "JWT_ACCESS_TOKEN_EXPIRY=3600000",
    "JWT_REFRESH_TOKEN_EXPIRY=604800000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private SecurityUtil securityUtil;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtility jwtUtility;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterCustomerRequest registerRequest;
    private UserAuthResponse userAuthResponse;
    private LoginRequest loginRequest;
    private LoginResponse loginResponse;

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

        userAuthResponse = new UserAuthResponse();
        userAuthResponse.setId("user-1");
        userAuthResponse.setEmail("customer@example.com");
        userAuthResponse.setRole("CUSTOMER");
        userAuthResponse.setIsActive(true);
        userAuthResponse.setIsEmailVerified(true);
        userAuthResponse.setForcePasswordChange(false);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("customer@example.com");
        loginRequest.setPassword("password123");

        loginResponse = new LoginResponse();
        loginResponse.setUserId("user-1");
        loginResponse.setEmail("customer@example.com");
        loginResponse.setRole("CUSTOMER");
        loginResponse.setAccessToken("access-token");
        loginResponse.setRefreshToken("refresh-token");
        loginResponse.setForcePasswordChange(false);
    }

    @Test
    void registerCustomer_ShouldReturnCreated() throws Exception {
        when(authService.registerCustomer(any(RegisterCustomerRequest.class)))
                .thenReturn(userAuthResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.message").value("Customer registered successfully"));
    }

    @Test
    void registerCustomer_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        registerRequest.setEmail(""); // Invalid email

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerCustomer_ShouldReturnConflict_WhenEmailExists() throws Exception {
        when(authService.registerCustomer(any(RegisterCustomerRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerManager_ShouldReturnCreated() throws Exception {
        RegisterManagerRequest request = new RegisterManagerRequest();
        request.setEmail("manager@example.com");
        request.setName("Manager Name");
        request.setRole("MANAGER");
        request.setPhone("1234567890");
        request.setDepartment("Operations");

        when(securityUtil.extractUserIdFromContext()).thenReturn("admin-1");
        when(authService.registerManager(any(RegisterManagerRequest.class), eq("admin-1")))
                .thenReturn(userAuthResponse);

        mockMvc.perform(post("/api/auth/admin/register-manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.message").value("Manager registered successfully"));
    }

    @Test
    void registerManager_ShouldReturnBadRequest_WhenAdminNotAuthenticated() throws Exception {
        RegisterManagerRequest request = new RegisterManagerRequest();
        request.setEmail("manager@example.com");

        when(securityUtil.extractUserIdFromContext()).thenReturn(null);

        mockMvc.perform(post("/api/auth/admin/register-manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerTechnician_ShouldReturnCreated() throws Exception {
        RegisterTechnicianRequest request = new RegisterTechnicianRequest();
        request.setEmail("tech@example.com");
        request.setName("Tech Name");
        request.setPhone("1234567890");

        when(authService.registerTechnician(any(RegisterTechnicianRequest.class)))
                .thenReturn(userAuthResponse);

        mockMvc.perform(post("/api/auth/admin/register-technician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.message").value("Technician registered successfully"));
    }

    @Test
    void registerTechnicianByManager_ShouldReturnCreated() throws Exception {
        RegisterTechnicianRequest request = new RegisterTechnicianRequest();
        request.setEmail("tech@example.com");
        request.setName("Tech Name");
        request.setPhone("1234567890");

        when(securityUtil.extractUserIdFromContext()).thenReturn("manager-1");
        when(authService.registerTechnicianByManager(any(RegisterTechnicianRequest.class), eq("manager-1")))
                .thenReturn(userAuthResponse);

        mockMvc.perform(post("/api/auth/manager/register-technician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.message").value("Technician registered successfully"));
    }

    @Test
    void login_ShouldReturnOk() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void login_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        loginRequest.setEmail(""); // Invalid email

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenInvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_ShouldReturnOk() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        TokenResponse tokenResponse = new TokenResponse("new-access-token", "refresh-token", 3600000L);
        when(authService.refreshAccessToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void logout_ShouldReturnNoContent() throws Exception {
        when(securityUtil.extractUserIdFromContext()).thenReturn("user-1");
        doNothing().when(authService).logout("user-1");

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_ShouldReturnBadRequest_WhenNotAuthenticated() throws Exception {
        when(securityUtil.extractUserIdFromContext()).thenReturn(null);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_ShouldReturnNoContent() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(securityUtil.extractUserIdFromContext()).thenReturn("user-1");
        when(authService.changePassword(eq("user-1"), any(ChangePasswordRequest.class)))
                .thenReturn(new MessageResponse("Password changed successfully"));

        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void changePassword_ShouldReturnBadRequest_WhenNotAuthenticated() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(securityUtil.extractUserIdFromContext()).thenReturn(null);

        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentUser_ShouldReturnOk() throws Exception {
        UserMeResponse userMeResponse = UserMeResponse.builder()
                .id("user-1")
                .email("customer@example.com")
                .role("CUSTOMER")
                .build();

        when(securityUtil.extractUserIdFromContext()).thenReturn("user-1");
        when(authService.getCurrentUser("user-1")).thenReturn(userMeResponse);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void getCurrentUser_ShouldReturnBadRequest_WhenNotAuthenticated() throws Exception {
        when(securityUtil.extractUserIdFromContext()).thenReturn(null);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPasswordEncoding_ShouldReturnOk() throws Exception {
        when(passwordEncoder.matches("plainPassword", "storedHash")).thenReturn(true);

        String requestBody = "{\"password\":\"plainPassword\",\"hash\":\"storedHash\"}";

        mockMvc.perform(post("/api/auth/debug/test-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordMatches").value(true))
                .andExpect(jsonPath("$.passwordEncoderType").exists());
    }
}

