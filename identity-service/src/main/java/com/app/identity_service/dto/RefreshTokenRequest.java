package com.app.identity_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Refresh Token Request DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

	@NotBlank(message = "Refresh token is required")
	@Size(min = 10, message = "Token must be valid JWT format")
	private String refreshToken;
}

