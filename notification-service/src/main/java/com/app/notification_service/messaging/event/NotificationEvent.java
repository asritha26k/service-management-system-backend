package com.app.notification_service.messaging.event;

import com.app.notification_service.enums.EmailType;
import com.app.notification_service.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String notificationId;
    private String userId;

    // USER INFO
    private String recipientEmail;

    // CHANGE DETAILS
    private String newEmail;
    private String temporaryPassword;
    private String oldRole;
    private String newRole;

    // METADATA
    private NotificationType notificationType; // EMAIL / SMS / PUSH
    private EmailType emailType; // EMAIL_CHANGE, PASSWORD_RESET, ROLE_CHANGE
    private String subject;
    private String message;

    private LocalDateTime createdAt = LocalDateTime.now();
}
