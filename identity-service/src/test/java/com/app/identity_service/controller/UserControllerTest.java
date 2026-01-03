package com.app.identity_service.controller;

import com.app.identity_service.dto.UserAuthResponse;
import com.app.identity_service.exception.GlobalExceptionHandler;
import com.app.identity_service.exception.ResourceNotFoundException;
import com.app.identity_service.service.UserService;
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

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {
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
    "LOG_LEVEL_ROOT=INFO",
    "LOG_LEVEL_APP=INFO",
    "SPRING_APPLICATION_NAME=identity-service-test",
    "SERVER_PORT=0"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtility jwtUtility;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UserAuthResponse userResponse;

    @BeforeEach
    void setUp() {
        userResponse = new UserAuthResponse();
        userResponse.setId("user-1");
        userResponse.setEmail("user@example.com");
        userResponse.setRole("CUSTOMER");
        userResponse.setIsActive(true);
        userResponse.setIsEmailVerified(true);
        userResponse.setForcePasswordChange(false);
    }

    @Test
    void getUsersByRole_ShouldReturnOk() throws Exception {
        List<UserAuthResponse> users = Arrays.asList(userResponse);
        when(userService.getUsersByRole("CUSTOMER")).thenReturn(users);

        mockMvc.perform(get("/api/users/role/CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("user-1"))
                .andExpect(jsonPath("$[0].email").value("user@example.com"))
                .andExpect(jsonPath("$[0].role").value("CUSTOMER"));
    }

    @Test
    void getUsersByRole_ShouldReturnBadRequest_WhenInvalidRole() throws Exception {
        when(userService.getUsersByRole("INVALID"))
                .thenThrow(new IllegalArgumentException("Invalid role: INVALID"));

        mockMvc.perform(get("/api/users/role/INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchUsers_ShouldReturnOk() throws Exception {
        List<UserAuthResponse> users = Arrays.asList(userResponse);
        when(userService.searchUsersByEmail("user")).thenReturn(users);

        mockMvc.perform(get("/api/users/search")
                        .param("email", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("user-1"))
                .andExpect(jsonPath("$[0].email").value("user@example.com"));
    }

    @Test
    void searchUsers_ShouldReturnEmptyList_WhenNoMatches() throws Exception {
        when(userService.searchUsersByEmail("nonexistent")).thenReturn(List.of());

        mockMvc.perform(get("/api/users/search")
                        .param("email", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getUserById_ShouldReturnOk() throws Exception {
        when(userService.getUserById("user-1")).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void getUserById_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        when(userService.getUserById("invalid-id"))
                .thenThrow(new ResourceNotFoundException("User", "id", "invalid-id"));

        mockMvc.perform(get("/api/users/invalid-id"))
                .andExpect(status().isNotFound());
    }
}

