package com.app.notification_service.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.notification_service.dto.IdMessageResponse;
import com.app.notification_service.dto.LoginCredentialsRequest;
import com.app.notification_service.dto.NotificationRequest;
import com.app.notification_service.dto.NotificationResponse;
import com.app.notification_service.entity.Notification;
import com.app.notification_service.exception.BadRequestException;
import com.app.notification_service.exception.NotificationFetchException;
import com.app.notification_service.security.RequestUser;
import com.app.notification_service.service.NotificationService;

import jakarta.validation.Valid;

// Notification Controller
// Handles all notification-related API endpoints
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Send a notification to a user
    @PostMapping("/send")
    public ResponseEntity<IdMessageResponse> sendNotification(@Valid @RequestBody NotificationRequest request) {
        log.info("POST /api/notifications/send - Sending notification for userId: {}", request.getUserId());
        log.debug("Notification type: {}, subject: {}", request.getType(), request.getSubject());
        NotificationResponse response = notificationService.sendNotification(request);
        log.info("Notification sent successfully with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new IdMessageResponse(response.getId(), "Notification sent successfully"));
    }

    // Send login credentials to a user via email
    @PostMapping("/send-credentials")
    public ResponseEntity<Void> sendCredentialEmail(@Valid @RequestBody LoginCredentialsRequest request) {
        log.info("POST /api/notifications/send-credentials - Sending credentials for email: {}", request.getEmail());
        log.debug("Role: {}", request.getRole());
        notificationService.sendCredentialEmail(request);
        log.info("Credentials email sent successfully to: {}", request.getEmail());
        return ResponseEntity.accepted().build();
    }

    // Get notifications for the authenticated user
    @GetMapping("/user")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(RequestUser user) {
        try {
            String userId = validateAndGetUserId(user);
            log.info("GET /api/notifications/user - Fetching notifications for userId: {}", userId);
            List<NotificationResponse> notifications =
                    notificationService.getNotificationsForUser(userId);
            log.info("Successfully retrieved {} notifications for user: {}", notifications.size(), userId);
            return ResponseEntity.ok(notifications);

        } catch (BadRequestException e) {
            log.warn("Bad request when fetching notifications: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching notifications for user: {}", e.getMessage(), e);
            throw new NotificationFetchException(
                    "Error fetching notifications: " + e.getMessage(), e
            );
        }
    }

    // Debug endpoint to fetch all notifications in the system
    @GetMapping("/debug/all")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        log.info("GET /api/notifications/debug/all - Fetching all notifications (DEBUG)");
        List<Notification> allNotifications = notificationService.getAllNotificationsDebug();
        log.info("Total notifications in system: {}", allNotifications.size());
        allNotifications.forEach(n -> log.debug("  - id: {}, userId: '{}', subject: {}", n.getId(), n.getUserId(), n.getSubject()));
        return ResponseEntity.ok(allNotifications.stream().map(this::mapToResponse).toList());
    }

    // Mark a notification as read by ID
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        log.info("PUT /api/notifications/{}/read - Marking notification as read", id);
        
        if (id == null || id.isBlank()) {
            log.warn("Notification ID cannot be empty");
            throw new BadRequestException("Notification ID cannot be empty");
        }
        
        log.debug("Marking notification {} as read", id);
        notificationService.markAsRead(id);
        log.info("Notification {} marked as read successfully", id);
        return ResponseEntity.noContent().build();
    }

    // Helper method to validate and extract user ID from RequestUser
    // @param user RequestUser object containing the user ID
    // @return validated userId
    // @throws BadRequestException if user ID is missing or blank
    private String validateAndGetUserId(RequestUser user) {
        if (user == null) {
            log.warn("RequestUser is null");
            throw new BadRequestException("User authentication is required");
        }
        
        String userId = user.userId();
        if (userId == null || userId.isBlank()) {
            log.warn("User ID is missing or blank");
            throw new BadRequestException("User ID is required");
        }
        
        log.debug("User ID validated: {}", userId);
        return userId;
    }

    // Convert Notification entity to NotificationResponse DTO
    private NotificationResponse mapToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setUserId(notification.getUserId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setSubject(notification.getSubject());
        response.setMessage(notification.getMessage());
        response.setRead(notification.isRead());
        response.setSentAt(notification.getSentAt());
        return response;
    }
}
