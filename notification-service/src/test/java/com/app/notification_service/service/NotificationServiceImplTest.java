package com.app.notification_service.service;

import com.app.notification_service.dto.LoginCredentialsRequest;
import com.app.notification_service.dto.NotificationRequest;
import com.app.notification_service.dto.NotificationResponse;
import com.app.notification_service.entity.Notification;
import com.app.notification_service.enums.EmailType;
import com.app.notification_service.enums.NotificationType;
import com.app.notification_service.exception.BadRequestException;
import com.app.notification_service.exception.NotFoundException;
import com.app.notification_service.messaging.NotificationEventPublisher;
import com.app.notification_service.messaging.event.NotificationEvent;
import com.app.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private NotificationRequest notificationRequest;
    private Notification notification;

    @BeforeEach
    void setUp() {
        notificationRequest = new NotificationRequest();
        notificationRequest.setUserId("user-1");
        notificationRequest.setType(NotificationType.IN_APP);
        notificationRequest.setSubject("Test Subject");
        notificationRequest.setMessage("Test Message");
        notificationRequest.setTitle("Test Title");

        notification = new Notification();
        notification.setId("notif-1");
        notification.setUserId("user-1");
        notification.setType(NotificationType.IN_APP);
        notification.setSubject("Test Subject");
        notification.setMessage("Test Message");
        notification.setTitle("Test Title");
        notification.setRead(false);
        notification.setSentAt(LocalDateTime.now());
    }

    @Test
    void sendNotification_ShouldCreateAndSaveNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        doNothing().when(eventPublisher).publish(any(NotificationEvent.class));

        NotificationResponse response = notificationService.sendNotification(notificationRequest);

        assertNotNull(response);
        assertEquals("notif-1", response.getId());
        assertEquals("user-1", response.getUserId());
        assertEquals(NotificationType.IN_APP, response.getType());
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(eventPublisher, times(1)).publish(any(NotificationEvent.class));
    }

    @Test
    void sendNotification_ShouldPublishEventWithCorrectData() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);

        notificationService.sendNotification(notificationRequest);

        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();
        assertEquals("notif-1", capturedEvent.getNotificationId());
        assertEquals("user-1", capturedEvent.getUserId());
        assertEquals(NotificationType.IN_APP, capturedEvent.getNotificationType());
        assertEquals(EmailType.NOTIFICATION, capturedEvent.getEmailType());
    }

    @Test
    void sendNotification_ShouldThrowBadRequest_WhenEmailTypeWithoutRecipient() {
        notificationRequest.setType(NotificationType.EMAIL);
        notificationRequest.setRecipientEmail(null);

        assertThrows(BadRequestException.class, () -> 
            notificationService.sendNotification(notificationRequest));
        
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void sendNotification_ShouldThrowBadRequest_WhenEmailTypeWithBlankRecipient() {
        notificationRequest.setType(NotificationType.EMAIL);
        notificationRequest.setRecipientEmail("   ");

        assertThrows(BadRequestException.class, () -> 
            notificationService.sendNotification(notificationRequest));
    }

    @Test
    void sendNotification_ShouldUseSubjectAsTitle_WhenTitleIsNull() {
        notificationRequest.setTitle(null);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        doNothing().when(eventPublisher).publish(any(NotificationEvent.class));

        NotificationResponse response = notificationService.sendNotification(notificationRequest);

        assertNotNull(response);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        assertEquals("Test Subject", notificationCaptor.getValue().getTitle());
    }

    @Test
    void sendCredentialEmail_ShouldPublishCredentialsEvent() {
        LoginCredentialsRequest request = new LoginCredentialsRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("tempPassword123");
        request.setRole("TECHNICIAN");

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        doNothing().when(eventPublisher).publish(any(NotificationEvent.class));

        notificationService.sendCredentialEmail(request);

        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();
        assertEquals(NotificationType.EMAIL, capturedEvent.getNotificationType());
        assertEquals("newuser@example.com", capturedEvent.getRecipientEmail());
        assertEquals("TECHNICIAN", capturedEvent.getNewRole());
        assertEquals("tempPassword123", capturedEvent.getTemporaryPassword());
        assertEquals(EmailType.CREDENTIALS, capturedEvent.getEmailType());
        assertTrue(capturedEvent.getMessage().contains("newuser@example.com"));
        assertTrue(capturedEvent.getMessage().contains("tempPassword123"));
        assertTrue(capturedEvent.getMessage().contains("TECHNICIAN"));
    }

    @Test
    void getNotificationsForUser_ShouldReturnNotifications() {
        Notification notif1 = new Notification();
        notif1.setId("notif-1");
        notif1.setUserId("user-1");
        notif1.setRead(false);

        Notification notif2 = new Notification();
        notif2.setId("notif-2");
        notif2.setUserId("user-1");
        notif2.setRead(true);

        when(notificationRepository.findByUserIdOrderBySentAtDesc("user-1"))
            .thenReturn(Arrays.asList(notif1, notif2));

        List<NotificationResponse> responses = notificationService.getNotificationsForUser("user-1");

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("notif-1", responses.get(0).getId());
        assertEquals("notif-2", responses.get(1).getId());
        verify(notificationRepository, times(1)).findByUserIdOrderBySentAtDesc("user-1");
    }

    @Test
    void getNotificationsForUser_ShouldReturnEmptyList_WhenUserIdIsNull() {
        List<NotificationResponse> responses = notificationService.getNotificationsForUser(null);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(notificationRepository, never()).findByUserIdOrderBySentAtDesc(anyString());
    }

    @Test
    void getNotificationsForUser_ShouldReturnEmptyList_WhenUserIdIsBlank() {
        List<NotificationResponse> responses = notificationService.getNotificationsForUser("   ");

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(notificationRepository, never()).findByUserIdOrderBySentAtDesc(anyString());
    }

    @Test
    void getNotificationsForUser_ShouldReturnEmptyList_WhenNoNotifications() {
        when(notificationRepository.findByUserIdOrderBySentAtDesc("user-1"))
            .thenReturn(List.of());

        List<NotificationResponse> responses = notificationService.getNotificationsForUser("user-1");

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void markAsRead_ShouldMarkNotificationAsRead() {
        notification.setRead(false);
        Notification updatedNotification = new Notification();
        updatedNotification.setId("notif-1");
        updatedNotification.setRead(true);

        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(updatedNotification);

        NotificationResponse response = notificationService.markAsRead("notif-1");

        assertNotNull(response);
        assertTrue(response.isRead());
        verify(notificationRepository, times(1)).findById("notif-1");
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void markAsRead_ShouldThrowNotFoundException_WhenNotificationNotFound() {
        when(notificationRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> 
            notificationService.markAsRead("invalid-id"));
        
        verify(notificationRepository, times(1)).findById("invalid-id");
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void getAllNotificationsDebug_ShouldReturnAllNotifications() {
        Notification notif1 = new Notification();
        notif1.setId("notif-1");
        Notification notif2 = new Notification();
        notif2.setId("notif-2");

        when(notificationRepository.findAll()).thenReturn(Arrays.asList(notif1, notif2));

        List<Notification> notifications = notificationService.getAllNotificationsDebug();

        assertNotNull(notifications);
        assertEquals(2, notifications.size());
        verify(notificationRepository, times(1)).findAll();
    }

    @Test
    void getAllNotificationsDebug_ShouldReturnEmptyList_WhenNoNotifications() {
        when(notificationRepository.findAll()).thenReturn(List.of());

        List<Notification> notifications = notificationService.getAllNotificationsDebug();

        assertNotNull(notifications);
        assertTrue(notifications.isEmpty());
    }

    @Test
    void sendNotification_ShouldSetReadToFalse() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        doNothing().when(eventPublisher).publish(any(NotificationEvent.class));

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        notificationService.sendNotification(notificationRequest);

        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        assertFalse(notificationCaptor.getValue().isRead());
    }

    @Test
    void sendNotification_ShouldSetSentAtTimestamp() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        doNothing().when(eventPublisher).publish(any(NotificationEvent.class));

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        notificationService.sendNotification(notificationRequest);

        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        assertNotNull(notificationCaptor.getValue().getSentAt());
    }
}

