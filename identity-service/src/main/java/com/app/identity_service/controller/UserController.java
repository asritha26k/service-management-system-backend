package com.app.identity_service.controller;

import com.app.identity_service.dto.PagedResponse;
import com.app.identity_service.dto.UserAuthResponse;
import com.app.identity_service.dto.UserDetailResponse;
import com.app.identity_service.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


//   User Controller
//   Handles user queries and admin operations
 
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	
	  //Get all users by role endpoint with pagination support and profile details
	 
	@GetMapping("/role/{role}")
	public ResponseEntity<PagedResponse<UserDetailResponse>> getUsersByRole(
			@PathVariable("role") String role,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size,
			@RequestParam(value = "sort", defaultValue = "email") String sortBy) {
		
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		PagedResponse<UserDetailResponse> users = userService.getUsersWithDetailsByRole(role, pageable);
		return ResponseEntity.ok(users);
	}

	
	 // Search users by email endpoint
	 
	@GetMapping("/search")
	public ResponseEntity<List<UserAuthResponse>> searchUsers(@RequestParam("email") String email) {
		List<UserAuthResponse> users = userService.searchUsersByEmail(email);
		return ResponseEntity.ok(users);
	}

	
	 // Get user by ID endpoint
	 
	@GetMapping("/{userId}")
	public ResponseEntity<UserAuthResponse> getUserById(@PathVariable("userId") String userId) {
		UserAuthResponse user = userService.getUserById(userId);
		return ResponseEntity.ok(user);
	}
}

