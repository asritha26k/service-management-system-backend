package com.app.notification_service.service;

import com.app.notification_service.dto.LoginCredentialsRequest;
import com.app.notification_service.dto.NotificationRequest;
import com.app.notification_service.dto.NotificationResponse;
import com.app.notification_service.entity.Notification;
import java.util.List;

public interface NotificationService {
    NotificationResponse sendNotification(NotificationRequest request);

    void sendCredentialEmail(LoginCredentialsRequest request);

    List<NotificationResponse> getNotificationsForUser(String userId);

    NotificationResponse markAsRead(String notificationId);
    
    List<Notification> getAllNotificationsDebug();
}
