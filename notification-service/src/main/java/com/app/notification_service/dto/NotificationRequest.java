package com.app.notification_service.dto;

import com.app.notification_service.enums.NotificationType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Notification Request DTO
// Represents a request to send a notification to a user
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotBlank(message = "User ID is required")
    @Size(min = 1, max = 50, message = "User ID must be between 1 and 50 characters")
    private String userId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @Size(min = 1, max = 150, message = "Title must be between 1 and 150 characters")
    private String title;

    @NotBlank(message = "Subject is required")
    @Size(min = 1, max = 150, message = "Subject must be between 1 and 150 characters")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
    private String message;

    @Email(message = "Recipient email must be valid")
    @Size(max = 150, message = "Recipient email must not exceed 150 characters")
    private String recipientEmail;
}
