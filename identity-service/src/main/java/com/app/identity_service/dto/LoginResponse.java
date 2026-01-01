package com.app.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Login Response DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

	private String userId;
	private String email;
	private String role;
	private String accessToken;
	private String refreshToken;
	private Boolean forcePasswordChange;

	public LoginResponse(String userId, String email, String role, String accessToken, String refreshToken) {
		this.userId = userId;
		this.email = email;
		this.role = role;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.forcePasswordChange = false;
	}
}

