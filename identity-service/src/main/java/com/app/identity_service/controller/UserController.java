package com.app.identity_service.controller;

import com.app.identity_service.dto.UserAuthResponse;
import com.app.identity_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


//   User Controller
//   Handles user queries and admin operations
 
@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired
	private UserService userService;

	
	  //Get all users by role endpoint
	 
	@GetMapping("/role/{role}")
	public ResponseEntity<List<UserAuthResponse>> getUsersByRole(@PathVariable("role") String role) {
		List<UserAuthResponse> users = userService.getUsersByRole(role);
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

