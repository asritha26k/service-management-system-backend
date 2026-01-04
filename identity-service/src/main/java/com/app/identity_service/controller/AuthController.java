package com.app.identity_service.controller;

import com.app.identity_service.dto.*;
import com.app.identity_service.dto.IdMessageResponse;
import com.app.identity_service.service.AuthService;
import com.app.identity_service.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
	private static final String TECH_SUCCESS_MSG = "Technician registered successfully";

	private final AuthService authService;

	private final SecurityUtil securityUtil;

	public AuthController(
			AuthService authService,
			SecurityUtil securityUtil) {
		this.authService = authService;
		this.securityUtil = securityUtil;
	}

	// customer endpoint to register
	@Operation(summary = "Register a new customer", description = "Public endpoint - No authentication required")
	@PostMapping("/register")
	public ResponseEntity<IdMessageResponse> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
		UserAuthResponse response = authService.registerCustomer(request);
		log.info("Customer registered successfully");
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new IdMessageResponse(response.getId(), "Customer registered successfully"));
	}

	// manager registration end point access to only admin
	@PostMapping("/admin/register-manager")
	public ResponseEntity<IdMessageResponse> registerManager(@Valid @RequestBody RegisterManagerRequest request) {
		String adminId = securityUtil.extractUserIdFromContext();
		if (adminId == null || adminId.isBlank()) {
			log.warn("Manager registration attempted without admin authentication");
			throw new IllegalArgumentException("Admin authentication is required");
		}
		log.debug("Manager registration by admin: {}", adminId);
		UserAuthResponse response = authService.registerManager(request, adminId);
		log.info("Manager registered successfully");
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new IdMessageResponse(response.getId(), "Manager registered successfully"));
	}

	// technician registration end point access to technician-service
	@PostMapping("/admin/register-technician")
	public ResponseEntity<IdMessageResponse> registerTechnician(@Valid @RequestBody RegisterTechnicianRequest request) {
		UserAuthResponse response = authService.registerTechnician(request);
		log.info(TECH_SUCCESS_MSG);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new IdMessageResponse(response.getId(), TECH_SUCCESS_MSG));
	}

	// technician registration end point access to managers
	@PostMapping("/manager/register-technician")
	public ResponseEntity<IdMessageResponse> registerTechnicianByManager(
			@Valid @RequestBody RegisterTechnicianRequest request) {
		String managerId = securityUtil.extractUserIdFromContext();
		if (managerId == null || managerId.isBlank()) {
			log.warn("Technician registration attempted without manager authentication");
			throw new IllegalArgumentException("Manager authentication is required");
		}
		log.debug("Technician registration by manager: {}", managerId);
		UserAuthResponse response = authService.registerTechnicianByManager(request, managerId);
		log.info("Technician registered successfully by manager");
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new IdMessageResponse(response.getId(), TECH_SUCCESS_MSG));
	}

	// login endpoint for the user
	@Operation(summary = "User login", description = "Public endpoint - No authentication required")
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		log.info("Login attempt received");
		LoginResponse response = authService.login(request);
		log.info("User logged in successfully");
		return ResponseEntity.ok(response);
	}

	// refresh token endpoint
	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		log.debug("Token refresh request received");
		TokenResponse response = authService.refreshAccessToken(request);
		log.info("Token refreshed successfully");

		return ResponseEntity.ok(response);
	}

	// logout endpoint
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

	// endpoint to change password
	@Operation(summary = "Change password", security = @SecurityRequirement(name = "bearer-jwt"))
	@PutMapping("/change-password")
	public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		String userId = securityUtil.extractUserIdFromContext();
		if (userId == null || userId.isBlank()) {
			log.warn("Change password attempted without authentication");
			throw new IllegalArgumentException("User authentication is required");
		}
		log.info("Password change requested for user: {}", userId);
		authService.changePassword(userId, request);
		return ResponseEntity.noContent().build();
	}

	// endpoint to get current user information
	@Operation(summary = "Get current user info", description = "Returns information about the authenticated user", security = @SecurityRequirement(name = "bearer-jwt"))
	@GetMapping("/me")
	public ResponseEntity<UserMeResponse> getCurrentUser() {
		String userId = securityUtil.extractUserIdFromContext();
		if (userId == null || userId.isBlank()) {
			log.warn("Get user info attempted without authentication");
			throw new IllegalArgumentException("User authentication is required");
		}
		UserMeResponse response = authService.getCurrentUser(userId);
		return ResponseEntity.ok(response);
	}
}
