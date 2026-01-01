package com.app.identity_service.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//dto to send credentials to mail
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsEmailRequest {
	private String email;
	private String temporaryPassword;
	private String userRole;
}

