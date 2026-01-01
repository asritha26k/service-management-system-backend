package com.app.notification_service.messaging.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.notification_service.entity.EmailLog;
import com.app.notification_service.enums.EmailStatus;
import com.app.notification_service.enums.EmailType;
import com.app.notification_service.enums.NotificationType;
import com.app.notification_service.config.RabbitConfig;
import com.app.notification_service.messaging.event.NotificationEvent;
import com.app.notification_service.repository.EmailLogRepository;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    public NotificationEventListener(JavaMailSender mailSender, EmailLogRepository emailLogRepository) {
        this.mailSender = mailSender;
        this.emailLogRepository = emailLogRepository;
    }

    @Transactional
    @RabbitListener(queues = RabbitConfig.NOTIFICATION_EMAIL_QUEUE)
    public void handleNotification(NotificationEvent event) {
        if (event.getNotificationType() != NotificationType.EMAIL) {
            log.info("Skipping non-email notification {}", event.getNotificationId());
            return;
        }


        if (event.getRecipientEmail() == null || event.getRecipientEmail().isBlank()) {
            log.warn("Missing recipient email for notification event {}", event.getNotificationId());
            return;
        }

        EmailLog emailLog = new EmailLog();
        emailLog.setRecipientEmail(event.getRecipientEmail());
        emailLog.setSubject(event.getSubject());
        emailLog.setEmailType(event.getEmailType() != null ? event.getEmailType() : EmailType.NOTIFICATION);
        emailLog.setStatus(EmailStatus.PENDING);

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(event.getRecipientEmail());
            mailMessage.setSubject(event.getSubject());
            mailMessage.setText(event.getMessage());
            mailSender.send(mailMessage);

            emailLog.setStatus(EmailStatus.SENT);
            log.info("Email sent to {} for notification {}", event.getRecipientEmail(), event.getNotificationId());
        } catch (MailException ex) {
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(ex.getMessage());
            log.error("Failed to send email to {}: {}", event.getRecipientEmail(), ex.getMessage());
        }

        emailLogRepository.save(emailLog);
    }
}
