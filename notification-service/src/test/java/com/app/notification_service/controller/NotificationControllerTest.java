package com.app.notification_service.controller;

import com.app.notification_service.dto.IdMessageResponse;
import com.app.notification_service.dto.LoginCredentialsRequest;
import com.app.notification_service.dto.NotificationRequest;
import com.app.notification_service.dto.NotificationResponse;
import com.app.notification_service.entity.Notification;
import com.app.notification_service.enums.NotificationType;
import com.app.notification_service.exception.BadRequestException;
import com.app.notification_service.exception.NotificationFetchException;
import com.app.notification_service.security.RequestUser;
import com.app.notification_service.security.RequestUserResolver;
import com.app.notification_service.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = NotificationController.class, excludeAutoConfiguration = {
    EurekaClientAutoConfiguration.class
})
@TestPropertySource(properties = {
    "logging.level.root=INFO",
    "logging.level.com.app.notification_service=INFO",
    "spring.application.name=notification-service-test",
    "server.port=0"
})
class NotificationControllerTest {

    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new RequestUserResolver());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private RequestUser testUser;
    private NotificationRequest notificationRequest;
    private NotificationResponse notificationResponse;

    @BeforeEach
    void setUp() {
        testUser = new RequestUser("user-1", "CUSTOMER");

        notificationRequest = new NotificationRequest();
        notificationRequest.setUserId("user-1");
        notificationRequest.setType(NotificationType.IN_APP);
        notificationRequest.setSubject("Test Subject");
        notificationRequest.setMessage("Test Message");
        notificationRequest.setTitle("Test Title");

        notificationResponse = new NotificationResponse();
        notificationResponse.setId("notif-1");
        notificationResponse.setUserId("user-1");
        notificationResponse.setType(NotificationType.IN_APP);
        notificationResponse.setSubject("Test Subject");
        notificationResponse.setMessage("Test Message");
        notificationResponse.setRead(false);
        notificationResponse.setSentAt(LocalDateTime.now());
    }

    @Test
    void sendNotification_ShouldReturnCreated() throws Exception {
        when(notificationService.sendNotification(any(NotificationRequest.class)))
                .thenReturn(notificationResponse);

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notificationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("notif-1"))
                .andExpect(jsonPath("$.message").value("Notification sent successfully"));
    }

    @Test
    void sendNotification_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        notificationRequest.setUserId(""); // Invalid: blank userId

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notificationRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendNotification_ShouldReturnBadRequest_WhenEmailTypeWithoutRecipient() throws Exception {
        notificationRequest.setType(NotificationType.EMAIL);
        notificationRequest.setRecipientEmail(null);

        when(notificationService.sendNotification(any(NotificationRequest.class)))
                .thenThrow(new BadRequestException("Recipient email is required for EMAIL type notifications"));

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notificationRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendCredentialEmail_ShouldReturnAccepted() throws Exception {
        LoginCredentialsRequest request = new LoginCredentialsRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("tempPassword123");
        request.setRole("TECHNICIAN");

        doNothing().when(notificationService).sendCredentialEmail(any(LoginCredentialsRequest.class));

        mockMvc.perform(post("/api/notifications/send-credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    void sendCredentialEmail_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        LoginCredentialsRequest request = new LoginCredentialsRequest();
        request.setEmail(""); // Invalid: blank email

        mockMvc.perform(post("/api/notifications/send-credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserNotifications_ShouldReturnOk() throws Exception {
        List<NotificationResponse> notifications = Arrays.asList(notificationResponse);
        when(notificationService.getNotificationsForUser("user-1")).thenReturn(notifications);

        mockMvc.perform(get("/api/notifications/user")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("notif-1"))
                .andExpect(jsonPath("$[0].userId").value("user-1"));
    }

    @Test
    void getUserNotifications_ShouldReturnBadRequest_WhenUserIsNull() throws Exception {
        when(notificationService.getNotificationsForUser(anyString()))
                .thenThrow(new BadRequestException("User authentication is required"));

        mockMvc.perform(get("/api/notifications/user"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserNotifications_ShouldReturnBadRequest_WhenUserIdIsBlank() throws Exception {
        when(notificationService.getNotificationsForUser(anyString()))
                .thenThrow(new BadRequestException("User ID is required"));

        mockMvc.perform(get("/api/notifications/user")
                        .header("X-User-Id", "")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserNotifications_ShouldHandleUnexpectedException() throws Exception {
        when(notificationService.getNotificationsForUser("user-1"))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/notifications/user")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAllNotifications_ShouldReturnOk() throws Exception {
        Notification notification = new Notification();
        notification.setId("notif-1");
        notification.setUserId("user-1");
        notification.setType(NotificationType.IN_APP);
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setRead(false);
        notification.setSentAt(LocalDateTime.now());

        List<Notification> notifications = Arrays.asList(notification);
        when(notificationService.getAllNotificationsDebug()).thenReturn(notifications);

        mockMvc.perform(get("/api/notifications/debug/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("notif-1"));
    }

    @Test
    void markAsRead_ShouldReturnNoContent() throws Exception {
        NotificationResponse updatedResponse = new NotificationResponse();
        updatedResponse.setId("notif-1");
        updatedResponse.setRead(true);

        when(notificationService.markAsRead("notif-1")).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/notifications/notif-1/read"))
                .andExpect(status().isNoContent());
    }


}

