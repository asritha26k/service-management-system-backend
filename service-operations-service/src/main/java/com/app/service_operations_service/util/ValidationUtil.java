package com.app.service_operations_service.util;

import com.app.service_operations_service.exception.BadRequestException;

public class ValidationUtil {
    private ValidationUtil() {
        // Utility class
    }

    public static void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " is required");
        }
    }

    public static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " cannot be empty");
        }
    }

    public static void validateNotNegative(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw new BadRequestException(fieldName + " cannot be negative");
        }
    }

    public static void validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BadRequestException(fieldName + " must be positive");
        }
    }

    public static void validateLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new BadRequestException(fieldName + " cannot exceed " + maxLength + " characters");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BadRequestException("Invalid email format");
        }
    }

    public static void validateInRange(Integer value, int min, int max, String fieldName) {
        if (value != null && (value < min || value > max)) {
            throw new BadRequestException(fieldName + " must be between " + min + " and " + max);
        }
    }
}
