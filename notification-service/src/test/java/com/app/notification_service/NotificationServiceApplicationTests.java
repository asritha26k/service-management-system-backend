package com.app.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.app.notification_service.repository.NotificationRepository;
import com.app.notification_service.repository.EmailLogRepository;

@SpringBootTest
class NotificationServiceApplicationTests {

	@MockBean
	private NotificationRepository notificationRepository;

	@MockBean
	private EmailLogRepository emailLogRepository;

	@Test
	void contextLoads() {
	}

}
