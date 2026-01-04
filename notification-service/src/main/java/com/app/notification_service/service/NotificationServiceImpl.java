package com.app.notification_service.service;

import com.app.notification_service.dto.LoginCredentialsRequest;
import com.app.notification_service.dto.NotificationRequest;
import com.app.notification_service.dto.NotificationResponse;
import com.app.notification_service.entity.Notification;
import com.app.notification_service.enums.EmailType;
import com.app.notification_service.enums.NotificationType;
import com.app.notification_service.exception.BadRequestException;
import com.app.notification_service.exception.NotFoundException;
import com.app.notification_service.messaging.NotificationEventPublisher;
import com.app.notification_service.messaging.event.NotificationEvent;
import com.app.notification_service.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Notification Service Implementation
// Handles notification creation, sending, and retrieval logic
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher eventPublisher;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
            NotificationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    // Send a notification to a user
    // @param request NotificationRequest containing notification details
    // @return NotificationResponse with created notification details
    // @throws BadRequestException if EMAIL type is used without recipient email
    @Override
    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("Processing notification request");

        // Validate email requirement for EMAIL type notifications
        if (request.getType() == NotificationType.EMAIL &&
                (request.getRecipientEmail() == null || request.getRecipientEmail().isBlank())) {
            log.warn("EMAIL notification requires recipient email but none provided");
            throw new BadRequestException("Recipient email is required for EMAIL type notifications");
        }

        log.debug("Creating notification entity");
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setTitle(request.getTitle() != null ? request.getTitle() : request.getSubject());
        notification.setSubject(request.getSubject());
        notification.setMessage(request.getMessage());
        notification.setRead(false);
        notification.setSentAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved successfully with ID: {}", saved.getId());

        // Publish notification event for async processing
        NotificationEvent event = new NotificationEvent();
        event.setNotificationId(saved.getId());
        event.setUserId(saved.getUserId());
        event.setNotificationType(saved.getType());
        event.setMessage(saved.getMessage());
        event.setRecipientEmail(request.getRecipientEmail());
        event.setSubject(saved.getSubject());
        event.setEmailType(EmailType.NOTIFICATION);
        eventPublisher.publish(event);
        log.info("Notification event published and queued");

        return mapToResponse(saved);
    }

    // Send login credentials via email to a new user
    // @param request LoginCredentialsRequest containing email, password, and role
    @Override
    public void sendCredentialEmail(LoginCredentialsRequest request) {
        log.info("Processing credential email request");

        NotificationEvent event = new NotificationEvent();
        event.setNotificationId("CRED-" + System.currentTimeMillis());
        event.setNotificationType(NotificationType.EMAIL);
        event.setUserId("SYSTEM_CREDENTIALS");
        event.setRecipientEmail(request.getEmail());
        event.setNewRole(request.getRole());
        event.setTemporaryPassword(request.getPassword());
        event.setEmailType(EmailType.CREDENTIALS);
        event.setSubject("Your Login Credentials");

        event.setMessage(String.format("""
                Hello,

                Your account has been created successfully.

                Email: %s
                Temporary Password: %s
                Role: %s

                Please log in with these credentials and change your password immediately.

                Best regards,
                Service Management System
                """,
                request.getEmail(),
                request.getPassword(),
                request.getRole()));

        eventPublisher.publish(event);
        log.info("Credential email event published");
    }

    // Get all notifications for a specific user
    // @param userId the user ID to fetch notifications for
    // @return List of NotificationResponse objects for the user
    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(String userId) {
        log.info("Fetching notifications");

        if (userId == null || userId.isBlank()) {
            log.warn("Invalid userId provided: null or blank");
            return List.of();
        }

        List<Notification> notifications = notificationRepository.findByUserIdOrderBySentAtDesc(userId);
        log.info("Retrieved {} notifications", notifications.size());

        if (notifications.isEmpty()) {
            log.warn("No notifications found");
        } else {
            log.debug("Notifications found");
            notifications.forEach(
                    n -> log.debug("  - id: {}, subject: {}, read: {}", n.getId(), n.getSubject(), n.isRead()));
        }

        return notifications.stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Mark a notification as read
    // @param notificationId the ID of the notification to mark as read
    // @return NotificationResponse with updated read status
    // @throws NotFoundException if notification is not found
    @Override
    @Transactional
    public NotificationResponse markAsRead(String notificationId) {
        log.info("Marking notification as read: {}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.warn("Notification not found with id: {}", notificationId);
                    return new NotFoundException("Notification not found for id: " + notificationId);
                });

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        log.info("Notification {} marked as read successfully", notificationId);
        return mapToResponse(updated);
    }

    // Get all notifications in the system (DEBUG endpoint)
    // @return List of all Notification entities
    @Override
    @Transactional(readOnly = true)
    public List<Notification> getAllNotificationsDebug() {
        log.debug("Fetching all notifications (DEBUG)");
        List<Notification> allNotifications = notificationRepository.findAll();
        log.info("Total notifications in system: {}", allNotifications.size());
        return allNotifications;
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
