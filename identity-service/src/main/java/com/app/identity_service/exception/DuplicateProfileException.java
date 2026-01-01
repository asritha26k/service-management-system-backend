package com.app.identity_service.exception;

// Exception thrown when attempting to create a duplicate profile for a user
public class DuplicateProfileException extends RuntimeException {
    public DuplicateProfileException(String userId) {
        super("Profile already exists for user with ID: " + userId);
    }

    public DuplicateProfileException(String message, Throwable cause) {
        super(message, cause);
    }
}
