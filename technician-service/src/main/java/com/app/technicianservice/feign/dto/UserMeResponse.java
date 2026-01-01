package com.app.technicianservice.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// User Me Response DTO
// Core user information for identity context
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMeResponse {

	private String id;
	private String email;
	private String role;
	private Boolean isActive;
	private Boolean isEmailVerified;
	private String name;
}
