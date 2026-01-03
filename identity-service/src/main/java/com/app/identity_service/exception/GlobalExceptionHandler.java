package com.app.identity_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

// Global Exception Handler
// Handles all exceptions across the application
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
			ResourceNotFoundException ex, WebRequest request) {
		log.warn("Resource not found: {}", ex.getMessage());
		ErrorResponse error = new ErrorResponse(
			HttpStatus.NOT_FOUND.value(),
			"Not Found",
			ex.getMessage(),
			request.getDescription(false).replace("uri=", "")
		);
		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
			InvalidCredentialsException ex, WebRequest request) {
		log.warn("Invalid credentials provided for authentication");
		ErrorResponse error = new ErrorResponse(
			HttpStatus.UNAUTHORIZED.value(),
			"Unauthorized",
			ex.getMessage(),
			request.getDescription(false).replace("uri=", "")
		);
		return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity<ErrorResponse> handleInvalidTokenException(
			InvalidTokenException ex, WebRequest request) {
		log.warn("Invalid or expired token: {}", ex.getMessage());
		ErrorResponse error = new ErrorResponse(
			HttpStatus.UNAUTHORIZED.value(),
			"Unauthorized",
			ex.getMessage(),
			request.getDescription(false).replace("uri=", "")
		);
		return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
			DuplicateResourceException ex, WebRequest request) {
		log.warn("Duplicate resource conflict: {}", ex.getMessage());
		ErrorResponse error = new ErrorResponse(
			HttpStatus.CONFLICT.value(),
			"Conflict",
			ex.getMessage(),
			request.getDescription(false).replace("uri=", "")
		);
		return new ResponseEntity<>(error, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(DuplicateProfileException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateProfileException(
			DuplicateProfileException ex, WebRequest request) {
		log.warn("Duplicate profile conflict: {}", ex.getMessage());
		ErrorResponse error = new ErrorResponse(
			HttpStatus.CONFLICT.value(),
			"Conflict",
			ex.getMessage(),
			request.getDescription(false).replace("uri=", "")
		);
		return new ResponseEntity<>(error, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
			IllegalArgumentException ex, WebRequest request) {
		log.warn("Illegal argument: {}", ex.getMessage());
		ErrorResponse error = new ErrorResponse(
			HttpStatus.BAD_REQUEST.value(),
			"Bad Request",
			ex.getMessage(),
			request.getDescription(false).replace("uri=", "")
		);
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleIllegalStateException(
			IllegalStateException ex, WebRequest request) {
		log.warn("Illegal state: {}", ex.getMessage());
		ErrorResponse error = new ErrorResponse(
			HttpStatus.CONFLICT.value(),
			"Conflict",
			ex.getMessage(),
			request.getDescription(false).replace("uri=", "")
		);
		return new ResponseEntity<>(error, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(
			MethodArgumentTypeMismatchException ex, WebRequest request) {
		String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
			ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
		log.warn(message);
		ErrorResponse error = new ErrorResponse(
			HttpStatus.BAD_REQUEST.value(),
			"Bad Request",
			message,
			request.getDescription(false).replace("uri=", "")
		);
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationException(
			MethodArgumentNotValidException ex, WebRequest request) {
		log.warn("Validation failed for request: {}", ex.getBindingResult().getObjectName());
		Map<String, Object> body = new HashMap<>();
		Map<String, String> errors = new HashMap<>();

		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
			log.warn("Validation error on field '{}': {}", fieldName, errorMessage);
		});

		body.put("timestamp", java.time.LocalDateTime.now().toString());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", "Validation Failed");
		body.put("errors", errors);
		body.put("path", request.getDescription(false).replace("uri=", ""));

		return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
	}

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
		log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        if (ex.getCause() instanceof InvalidCredentialsException invalidCredentialsException) {
            return handleInvalidCredentialsException(invalidCredentialsException, request);
        }

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please contact support.",
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
