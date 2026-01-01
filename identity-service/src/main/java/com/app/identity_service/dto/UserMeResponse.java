package com.app.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// User Me Response DTO
// Combines user auth and profile information
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMeResponse {

	private String id;
	private String email;
	private String role;
	private Boolean isActive;
	private Boolean isEmailVerified;
	private Boolean forcePasswordChange;
	private String name;
	private String phone;
	private String address;
	private String city;
	private String state;
	private String pincode;
	private String department;
}

