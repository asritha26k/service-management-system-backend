package com.app.identity_service.exception;

// Custom exception for duplicate resource
public class DuplicateResourceException extends RuntimeException {

	public DuplicateResourceException(String message) {
		super(message);
	}
}

