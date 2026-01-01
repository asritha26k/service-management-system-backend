package com.app.service_operations_service.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class NotificationRequest {

    @NotNull
    private String userId;

    @NotNull
    private NotificationType type;

    @NotBlank
    @Size(max = 150)
    private String title;

    private String subject;
    @NotBlank
    @Size(max = 2000)
    private String message;

    @Email
    @Size(max = 150)
    private String recipientEmail;
}
