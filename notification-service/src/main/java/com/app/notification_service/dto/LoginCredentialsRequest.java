package com.app.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Login Credentials Request DTO
// Used to send login credentials to a new user via email
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginCredentialsRequest {

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @ToString.Exclude
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(ADMIN|MANAGER|TECHNICIAN|CUSTOMER)$", message = "Role must be one of: ADMIN, MANAGER, TECHNICIAN, CUSTOMER")
    private String role;
}
