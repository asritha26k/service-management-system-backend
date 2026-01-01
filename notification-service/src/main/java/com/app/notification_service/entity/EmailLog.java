package com.app.notification_service.entity;

import java.time.LocalDateTime;

import com.app.notification_service.enums.EmailStatus;
import com.app.notification_service.enums.EmailType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "email_logs")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 150)
    private String recipientEmail;

    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailType emailType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailStatus status = EmailStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(length = 500)
    private String errorMessage;
}
