package com.app.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Credential Email Request DTO
// Represents a request to send credentials via email
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialEmailRequest {

    @Email(message = "Recipient email must be valid")
    @NotBlank(message = "Recipient email is required")
    @Size(max = 255, message = "Recipient email must not exceed 255 characters")
    private String recipientEmail;

    @NotBlank(message = "Subject is required")
    @Size(min = 1, max = 200, message = "Subject must be between 1 and 200 characters")
    private String subject;

    @NotBlank(message = "Body is required")
    @Size(min = 1, max = 4000, message = "Body must be between 1 and 4000 characters")
    private String body;
}
