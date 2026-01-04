package com.app.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.app.notification_service.repository.NotificationRepository;
import com.app.notification_service.repository.EmailLogRepository;

@SpringBootTest
class NotificationServiceApplicationTests {

	@MockitoBean
	private NotificationRepository notificationRepository;

	@MockitoBean
	private EmailLogRepository emailLogRepository;

	@Test
	void contextLoads() {
	}

}
