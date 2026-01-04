package com.app.identity_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Change Password Request DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

	@ToString.Exclude
	@NotBlank(message = "Current password is required")
	@Size(min = 6, max = 100, message = "Current password must be between 6 and 100 characters")
	private String currentPassword;

	@ToString.Exclude
	@NotBlank(message = "New password is required")
	@Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters")
	private String newPassword;

	@ToString.Exclude
	@NotBlank(message = "Password confirmation is required")
	@Size(min = 6, max = 100, message = "Confirmation password must be between 6 and 100 characters")
	private String confirmPassword;
}
