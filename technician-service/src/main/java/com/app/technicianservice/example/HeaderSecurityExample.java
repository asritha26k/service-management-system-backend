package com.app.technicianservice.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.technicianservice.util.UserContext;

// EXAMPLE: How to use header-based authentication and authorization.
// 
// This demonstrates the refactored security model where:
// - JWT validation is done by API Gateway
// - User identity is passed via X-User-Id and X-User-Role headers
// - This service trusts headers from the gateway
// - No JWT parsing or Spring Security filters in this service
@RestController
@RequestMapping("/api/example")
public class HeaderSecurityExample {

    // Example 1: Public endpoint (no authentication required)
    // Anyone can call this - no headers needed
    @GetMapping("/public")
    public String publicEndpoint() {
        return "This is a public endpoint - no authentication required";
    }

    // Example 2: Authenticated endpoint
    // Requires X-User-Id header - validates user is authenticated
    @GetMapping("/authenticated")
    public String authenticatedEndpoint(
            @RequestHeader(UserContext.HEADER_USER_ID) String userId) {
        
        // Validate that user is authenticated
        UserContext.requireAuthenticated(userId);
        
        return "Hello user: " + userId;
    }

    // Example 3: Role-based authorization
    // Only ADMIN and MANAGER roles can access this endpoint
    @GetMapping("/admin-only")
    public String adminOnlyEndpoint(
            @RequestHeader(UserContext.HEADER_USER_ID) String userId,
            @RequestHeader(UserContext.HEADER_USER_ROLE) String userRole) {
        
        // Validate user has required role
        UserContext.requireRole(userRole, UserContext.Role.ADMIN, UserContext.Role.MANAGER);
        
        return "Admin/Manager action performed by: " + userId;
    }

    // Example 4: Ownership validation
    // Users can only access their own resources unless they are admin/manager
    // 
    // Use case: User updating their own profile
    @PutMapping("/profile/{profileUserId}")
    public String updateOwnProfile(
            @RequestHeader(UserContext.HEADER_USER_ID) String userId,
            @RequestHeader(UserContext.HEADER_USER_ROLE) String userRole,
            @PathVariable String profileUserId,
            @RequestBody String data) {
        
        // Verify user owns this profile OR has admin/manager role
        UserContext.requireOwnershipOrAdmin(userId, userRole, profileUserId);
        
        return "Profile updated for: " + profileUserId + " by: " + userId;
    }

    // Example 5: Optional role check for conditional logic
    // Different behavior based on user role
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestHeader(UserContext.HEADER_USER_ID) String userId,
            @RequestHeader(value = UserContext.HEADER_USER_ROLE, required = false) String userRole) {
        
        UserContext.requireAuthenticated(userId);
        
        // Check if user has elevated privileges
        if (UserContext.isAdminOrManager(userRole)) {
            return "Admin Dashboard with full access";
        } else {
            return "User Dashboard with limited access";
        }
    }

    // Example 6: Self-action endpoint (removed userId from path)
    // Before: POST /api/users/{userId}/action
    // After:  POST /api/users/me/action
    // 
    // User ID is derived from header instead of path variable
    @PostMapping("/me/action")
    public String selfAction(
            @RequestHeader(UserContext.HEADER_USER_ID) String userId,
            @RequestBody String actionData) {
        
        UserContext.requireAuthenticated(userId);
        
        // Use userId from header for the action
        return "Action performed by user: " + userId;
    }

    // Example 7: Manager approving something
    // Manager ID comes from header, not from IdentityService call
    @PostMapping("/approve/{resourceId}")
    public String approveResource(
            @RequestHeader(UserContext.HEADER_USER_ID) String managerId,
            @RequestHeader(UserContext.HEADER_USER_ROLE) String userRole,
            @PathVariable String resourceId) {
        
        // Ensure only managers/admins can approve
        UserContext.requireRole(userRole, UserContext.Role.ADMIN, UserContext.Role.MANAGER);
        
        // managerId comes from header - no need to call identity service
        return "Resource " + resourceId + " approved by manager: " + managerId;
    }
}
