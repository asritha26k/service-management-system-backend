package com.app.identity_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Update User Profile Request DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

	@NotBlank(message = "Name is required")
	@Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
	private String name;

	@Size(max = 20, message = "Phone must not exceed 20 characters")
	@Pattern(regexp = "^[0-9+\\-\\s]*$", message = "Phone must contain only digits, +, -, or spaces")
	private String phone;

	@Size(max = 500, message = "Address must not exceed 500 characters")
	private String address;

	@Size(max = 100, message = "City must not exceed 100 characters")
	private String city;

	@Size(max = 100, message = "State must not exceed 100 characters")
	private String state;

	@Size(max = 10, message = "Pincode must not exceed 10 characters")
	@Pattern(regexp = "^[0-9]*$", message = "Pincode must contain only digits")
	private String pincode;

	public UpdateUserProfileRequest(String name) {
		this.name = name;
	}
}

