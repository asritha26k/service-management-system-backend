package com.app.identity_service.controller;

import com.app.identity_service.dto.*;
import com.app.identity_service.service.AuthService;
import com.app.identity_service.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	@Autowired
	private AuthService authService;

	@Autowired
	private SecurityUtil securityUtil;

	@Autowired
	private PasswordEncoder passwordEncoder;

	//customer endpoint to register
	@Operation(summary = "Register a new customer", description = "Public endpoint - No authentication required")
	@PostMapping("/register")
	public ResponseEntity<UserAuthResponse> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
		log.info("Customer registration request for email: {}", request.getEmail());
		UserAuthResponse response = authService.registerCustomer(request);
		log.info("Customer registered successfully: {}", request.getEmail());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	//manager registration end point access to only admin
	@PostMapping("/admin/register-manager")
	public ResponseEntity<UserAuthResponse> registerManager(@Valid @RequestBody RegisterManagerRequest request) {
		log.info("Manager registration request for email: {}", request.getEmail());
		String adminId = securityUtil.extractUserIdFromContext();
		if (adminId == null || adminId.isBlank()) {
			log.warn("Manager registration attempted without admin authentication");
			throw new IllegalArgumentException("Admin authentication is required");
		}
		log.debug("Manager registration by admin: {}", adminId);
		UserAuthResponse response = authService.registerManager(request, adminId);
		log.info("Manager registered successfully: {}", request.getEmail());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	//technician registration end point access to technician-service
	@PostMapping("/admin/register-technician")
	public ResponseEntity<UserAuthResponse> registerTechnician(@Valid @RequestBody RegisterTechnicianRequest request) {
		log.info("Technician registration request for email: {}", request.getEmail());
		UserAuthResponse response = authService.registerTechnician(request);
		log.info("Technician registered successfully: {}", request.getEmail());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	//technician registration end point access to managers
	@PostMapping("/manager/register-technician")
	public ResponseEntity<UserAuthResponse> registerTechnicianByManager(@Valid @RequestBody RegisterTechnicianRequest request) {
		log.info("Technician registration request by manager for email: {}", request.getEmail());
		String managerId = securityUtil.extractUserIdFromContext();
		if (managerId == null || managerId.isBlank()) {
			log.warn("Technician registration attempted without manager authentication");
			throw new IllegalArgumentException("Manager authentication is required");
		}
		log.debug("Technician registration by manager: {}", managerId);
		UserAuthResponse response = authService.registerTechnicianByManager(request, managerId);
		log.info("Technician registered successfully by manager: {}", request.getEmail());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	//login endpoint for the user
	@Operation(summary = "User login", description = "Public endpoint - No authentication required")
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());
        LoginResponse response = authService.login(request);
		log.info("User logged in successfully: {}", request.getEmail());
		return ResponseEntity.ok(response);
	}

	//refresh token endpoint
	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		log.debug("Token refresh request received");
		TokenResponse response = authService.refreshAccessToken(request);
		log.info("Token refreshed successfully");

		return ResponseEntity.ok(response);
	}

	//logout endpoint
	@Operation(summary = "Logout user", security = @SecurityRequirement(name = "bearer-jwt"))
	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		String userId = securityUtil.extractUserIdFromContext();
		if (userId == null || userId.isBlank()) {
			log.warn("Logout attempted without authentication");
			throw new IllegalArgumentException("User authentication is required");
		}
		log.info("User logged out: {}", userId);
		authService.logout(userId);
		return ResponseEntity.noContent().build();
	}

	//endpoint to change password
	@Operation(summary = "Change password", security = @SecurityRequirement(name = "bearer-jwt"))
	@PutMapping("/change-password")
	public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		String userId = securityUtil.extractUserIdFromContext();
		if (userId == null || userId.isBlank()) {
			log.warn("Change password attempted without authentication");
			throw new IllegalArgumentException("User authentication is required");
		}
		log.info("Password change requested for user: {}", userId);
		return ResponseEntity.ok(authService.changePassword(userId, request));
	}

	//endpoint to get current user information
	@Operation(summary = "Get current user info", description = "Returns information about the authenticated user", security = @SecurityRequirement(name = "bearer-jwt"))
	@GetMapping("/me")
	public ResponseEntity<UserMeResponse> getCurrentUser() {
		String userId = securityUtil.extractUserIdFromContext();
		if (userId == null || userId.isBlank()) {
			log.warn("Get user info attempted without authentication");
			throw new IllegalArgumentException("User authentication is required");
		}
		log.debug("Fetching user info for: {}", userId);
		UserMeResponse response = authService.getCurrentUser(userId);
		return ResponseEntity.ok(response);
	}


	 //Debug endpoint to test password encoding
	@PostMapping("/debug/test-password")
	public ResponseEntity<Map<String, Object>> testPasswordEncoding(@RequestBody Map<String, String> request) {
		String plainPassword = request.get("password");
		String storedHash = request.get("hash");

		Map<String, Object> result = new HashMap<>();
		result.put("plainPassword", plainPassword);
		result.put("storedHash", storedHash);
		result.put("passwordEncoderType", passwordEncoder.getClass().getName());

		try {
			boolean matches = passwordEncoder.matches(plainPassword, storedHash);
			result.put("passwordMatches", matches);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			result.put("errorType", e.getClass().getName());
		}

		return ResponseEntity.ok(result);
	}
}
