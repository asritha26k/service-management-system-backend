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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ERROR_CONFLICT = "Conflict";
    private static final String ERROR_BAD_REQUEST = "Bad Request";
    private static final String ERROR_UNAUTHORIZED = "Unauthorized";
    private static final String ERROR_NOT_FOUND = "Not Found";
    private static final String ERROR_INTERNAL = "Internal Server Error";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ERROR_NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
            InvalidCredentialsException ex, WebRequest request) {

        log.warn("Invalid credentials provided");
        return buildError(HttpStatus.UNAUTHORIZED, ERROR_UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(
            InvalidTokenException ex, WebRequest request) {

        log.warn("Invalid or expired token");
        return buildError(HttpStatus.UNAUTHORIZED, ERROR_UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {

        log.warn("Duplicate resource: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ERROR_CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateProfileException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProfileException(
            DuplicateProfileException ex, WebRequest request) {

        log.warn("Duplicate profile: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ERROR_CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ERROR_BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {

        log.warn("Illegal state: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ERROR_CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String message = String.format(
                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType().getSimpleName()
        );

        log.warn(message);
        return buildError(HttpStatus.BAD_REQUEST, ERROR_BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });

        Map<String, Object> body = new HashMap<>();
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

        log.error("Unexpected error occurred", ex);

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ERROR_INTERNAL,
                "An unexpected error occurred. Please contact support.",
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String error,
            String message,
            WebRequest request) {

        ErrorResponse response = new ErrorResponse(
                status.value(),
                error,
                message,
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(response, status);
    }
}
