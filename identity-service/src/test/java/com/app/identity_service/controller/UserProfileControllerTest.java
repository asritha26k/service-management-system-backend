package com.app.identity_service.controller;

import com.app.identity_service.dto.UpdateUserProfileRequest;
import com.app.identity_service.dto.UserProfileResponse;
import com.app.identity_service.exception.GlobalExceptionHandler;
import com.app.identity_service.exception.ResourceNotFoundException;
import com.app.identity_service.service.UserProfileService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserProfileController.class, excludeAutoConfiguration = {
        EurekaClientAutoConfiguration.class,
        SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "logging.level.root=INFO",
        "logging.level.com.app.identity_service=INFO",
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.application.name=identity-service-test",
        "server.port=0",
        "LOG_LEVEL_ROOT=INFO",
        "LOG_LEVEL_APP=INFO",
        "SPRING_APPLICATION_NAME=identity-service-test",
        "SERVER_PORT=0"
})
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private SecurityUtil securityUtil;

    @MockBean
    private JwtUtility jwtUtility;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UpdateUserProfileRequest profileRequest;
    private UserProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        profileRequest = new UpdateUserProfileRequest();
        profileRequest.setName("John Doe");
        profileRequest.setPhone("1234567890");
        profileRequest.setAddress("123 Main St");
        profileRequest.setCity("New York");
        profileRequest.setState("NY");
        profileRequest.setPincode("10001");

        profileResponse = UserProfileResponse.builder()
                .id("profile-1")
                .userId("user-1")
                .name("John Doe")
                .phone("1234567890")
                .address("123 Main St")
                .city("New York")
                .state("NY")
                .pincode("10001")
                .build();
    }

    @Test
    void createProfile_ShouldReturnCreated() throws Exception {
        when(securityUtil.extractUserIdFromContext()).thenReturn("user-1");
        when(userProfileService.createProfile(eq("user-1"), any(UpdateUserProfileRequest.class)))
                .thenReturn(profileResponse);

        mockMvc.perform(post("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.message").value("User profile created successfully"));
    }

    @Test
    void createProfile_ShouldReturnBadRequest_WhenNotAuthenticated() throws Exception {
        when(securityUtil.extractUserIdFromContext()).thenReturn(null);

        mockMvc.perform(post("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProfileByUserId_ShouldReturnOk() throws Exception {
        when(userProfileService.getProfileByUserId("user-1")).thenReturn(profileResponse);

        mockMvc.perform(get("/api/users/profile/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void getProfileByUserId_ShouldReturnNotFound_WhenProfileNotFound() throws Exception {
        when(userProfileService.getProfileByUserId("invalid-id"))
                .thenThrow(new ResourceNotFoundException("UserProfile", "userId", "invalid-id"));

        mockMvc.perform(get("/api/users/profile/invalid-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyProfile_ShouldReturnOk() throws Exception {
        when(securityUtil.extractUserIdFromContext()).thenReturn("user-1");
        when(userProfileService.getProfileByUserId("user-1")).thenReturn(profileResponse);

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void getMyProfile_ShouldReturnBadRequest_WhenNotAuthenticated() throws Exception {
        when(securityUtil.extractUserIdFromContext()).thenReturn(null);

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_ShouldReturnNoContent() throws Exception {
        when(userProfileService.updateProfile(eq("user-1"), any(UpdateUserProfileRequest.class)))
                .thenReturn(profileResponse);

        mockMvc.perform(put("/api/users/profile/user-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateProfile_ShouldReturnBadRequest_WhenUserIdIsBlank() throws Exception {
        mockMvc.perform(put("/api/users/profile/ ")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMyProfile_ShouldReturnNoContent() throws Exception {
        when(securityUtil.extractUserIdFromContext()).thenReturn("user-1");
        when(userProfileService.updateProfile(eq("user-1"), any(UpdateUserProfileRequest.class)))
                .thenReturn(profileResponse);

        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateMyProfile_ShouldReturnBadRequest_WhenNotAuthenticated() throws Exception {
        when(securityUtil.extractUserIdFromContext()).thenReturn(null);

        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isBadRequest());
    }
}
