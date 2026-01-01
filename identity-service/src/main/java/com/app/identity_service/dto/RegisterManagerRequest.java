package com.app.identity_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Register Manager Request DTO
// Used by Admin to register Manager
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterManagerRequest {

	@Email(message = "Email must be valid")
	@NotBlank(message = "Email is required")
	@Size(max = 255, message = "Email must not exceed 255 characters")
	private String email;

	@NotBlank(message = "Name is required")
	@Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
	private String name;

	@NotBlank(message = "Role is required")
	@Pattern(regexp = "MANAGER", message = "Role must be MANAGER for this endpoint")
	private String role;

	@Size(max = 20, message = "Phone must not exceed 20 characters")
	@Pattern(regexp = "^[0-9+\\-\\s]*$", message = "Phone must contain only digits, +, -, or spaces")
	private String phone;

	@Size(max = 100, message = "Department must not exceed 100 characters")
	private String department;
}

