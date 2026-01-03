package com.app.technicianservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.app.technicianservice.repository.TechnicianProfileRepository;
import com.app.technicianservice.repository.TechnicianApplicationRepository;
import com.app.technicianservice.repository.TechnicianScheduleRepository;
import com.app.technicianservice.repository.TechnicianRatingRepository;

@SpringBootTest
class TechnicianServiceApplicationTests {

	@MockBean
	private TechnicianProfileRepository technicianProfileRepository;

	@MockBean
	private TechnicianApplicationRepository technicianApplicationRepository;

	@MockBean
	private TechnicianScheduleRepository technicianScheduleRepository;

	@MockBean
	private TechnicianRatingRepository technicianRatingRepository;

	@Test
	void contextLoads() {
	}
}


