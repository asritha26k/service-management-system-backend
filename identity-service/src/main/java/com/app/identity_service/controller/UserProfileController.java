package com.app.identity_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.identity_service.dto.UpdateUserProfileRequest;
import com.app.identity_service.dto.UserProfileResponse;
import com.app.identity_service.service.UserProfileService;
import com.app.identity_service.util.SecurityUtil;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//handles user profile endpoints
@RestController
@RequestMapping("/api/users/profile")
public class UserProfileController {

	private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);

	@Autowired
	private UserProfileService userProfileService;

	@Autowired
	private SecurityUtil securityUtil;

	//creates user profile endpoints
	@PostMapping
	public ResponseEntity<UserProfileResponse> createProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
		String userId = securityUtil.extractUserIdFromContext();
		if (userId == null || userId.isBlank()) {
			log.warn("Create profile attempted without authentication");
			throw new IllegalArgumentException("User authentication is required");
		}
		log.info("Creating user profile for user: {}", userId);
		UserProfileResponse response = userProfileService.createProfile(userId, request);
		log.info("User profile created successfully for: {}", userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	//get user profile endpoint
	@GetMapping("/{userId}")
	public ResponseEntity<UserProfileResponse> getProfileByUserId(@PathVariable String userId) {
		if (userId == null || userId.isBlank()) {
			log.warn("Get profile attempted with invalid user ID");
			throw new IllegalArgumentException("User ID is required");
		}
		log.debug("Fetching user profile for: {}", userId);
		UserProfileResponse response = userProfileService.getProfileByUserId(userId);
		return ResponseEntity.ok(response);
	}

	
	 // Get own profile (self-service - extracts userId from JWT token) 
	@GetMapping
	public ResponseEntity<UserProfileResponse> getMyProfile() {
		String userId = securityUtil.extractUserIdFromContext();
		if (userId == null || userId.isBlank()) {
			log.warn("Get own profile attempted without authentication");
			throw new IllegalArgumentException("User authentication is required");
		}
		log.debug("Fetching own profile for user: {}", userId);
		UserProfileResponse response = userProfileService.getProfileByUserId(userId);
		return ResponseEntity.ok(response);
	}


	 //Update user profile (admin/manager can update any user)

	@PutMapping("/{userId}")
	public ResponseEntity<UserProfileResponse> updateProfile(
			@PathVariable String userId,
			@Valid @RequestBody UpdateUserProfileRequest request) {
		if (userId == null || userId.isBlank()) {
			log.warn("Update profile attempted with invalid user ID");
			throw new IllegalArgumentException("User ID is required");
		}
		log.info("Updating user profile for: {}", userId);
		UserProfileResponse response = userProfileService.updateProfile(userId, request);
		log.info("User profile updated successfully for: {}", userId);
		return ResponseEntity.ok(response);
	}

	
	 // Update own profile (self-service - extracts userId from JWT token)
	 
	@PutMapping
	public ResponseEntity<UserProfileResponse> updateMyProfile(
			@Valid @RequestBody UpdateUserProfileRequest request) {
		String userId = securityUtil.extractUserIdFromContext();
		if (userId == null || userId.isBlank()) {
			log.warn("Update own profile attempted without authentication");
			throw new IllegalArgumentException("User authentication is required");
		}
		log.info("User updating own profile: {}", userId);
		UserProfileResponse response = userProfileService.updateProfile(userId, request);
		log.info("Own profile updated successfully for: {}", userId);
		return ResponseEntity.ok(response);
	}
}
