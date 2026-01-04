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
import com.app.notification_service.security.RequestUser;
import com.app.notification_service.service.NotificationService;

import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<IdMessageResponse> sendNotification(
            @Valid @RequestBody NotificationRequest request) {

        log.info("POST /api/notifications/send - Sending notification");

        NotificationResponse response = notificationService.sendNotification(request);

        log.info("Notification sent successfully with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new IdMessageResponse(response.getId(), "Notification sent successfully"));
    }

    @PostMapping("/send-credentials")
    public ResponseEntity<Void> sendCredentialEmail(
            @Valid @RequestBody LoginCredentialsRequest request) {

        log.info("POST /api/notifications/send-credentials - Sending credentials");

        notificationService.sendCredentialEmail(request);

        log.info("Credentials email sent successfully");
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/user")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(RequestUser user) {

        String userId = validateAndGetUserId(user);

        log.info("GET /api/notifications/user - Fetching notifications");

        List<NotificationResponse> notifications =
                notificationService.getNotificationsForUser(userId);

        log.info("Successfully retrieved {} notifications", notifications.size());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/debug/all")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {

        log.info("GET /api/notifications/debug/all - Fetching all notifications (DEBUG)");

        List<Notification> allNotifications =
                notificationService.getAllNotificationsDebug();

        log.info("Total notifications in system: {}", allNotifications.size());

        allNotifications.forEach(n ->
                log.debug("  - id: {}, userId: '{}', subject: {}",
                        n.getId(), n.getUserId(), n.getSubject()));

        return ResponseEntity.ok(
                allNotifications.stream().map(this::mapToResponse).toList()
        );
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") String id) {

        if (id == null || id.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Notification id must not be empty");
        }

        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

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
