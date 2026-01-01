package com.app.identity_service.exception;

// Custom exception for invalid credentials
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException(String message) {
		super(message);
	}
}

