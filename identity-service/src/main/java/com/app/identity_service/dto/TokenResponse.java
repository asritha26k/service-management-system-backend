package com.app.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Token Response DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

	private String accessToken;
	private String refreshToken;
	private Long expiresIn;
}

