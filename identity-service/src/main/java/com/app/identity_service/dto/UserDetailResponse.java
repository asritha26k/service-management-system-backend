package com.app.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Enhanced User Response DTO with profile information
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {

	private String id;
	private String email;
	private String role;
	private Boolean isActive;
	private Boolean isEmailVerified;
	private Boolean forcePasswordChange;
	
	// Profile information
	private String name;
	private String phone;
	private String address;
	private String city;
	private String state;
	private String pincode;
}
