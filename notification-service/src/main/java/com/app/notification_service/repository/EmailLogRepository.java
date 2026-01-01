package com.app.notification_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.notification_service.entity.EmailLog;

public interface EmailLogRepository extends JpaRepository<EmailLog, String> {
}
