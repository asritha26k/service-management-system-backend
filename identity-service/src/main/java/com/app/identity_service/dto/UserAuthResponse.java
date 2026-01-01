package com.app.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// User Auth Response DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthResponse {

	private String id;
	private String email;
	private String role;
	private Boolean isActive;
	private Boolean isEmailVerified;
	private Boolean forcePasswordChange;
}

