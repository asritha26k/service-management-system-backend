package com.app.identity_service.exception;

// Exception for invalid or expired tokens
public class InvalidTokenException extends RuntimeException {
	
	public InvalidTokenException(String message) {
		super(message);
	}

	public InvalidTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
