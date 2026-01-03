package com.app.service_operations_service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.lang.NonNull;
import com.app.service_operations_service.repository.ServiceCategoryRepository;
import com.app.service_operations_service.repository.ServiceItemRepository;
import com.app.service_operations_service.repository.InvoiceRepository;
import com.app.service_operations_service.repository.ServiceRequestRepository;
import com.app.service_operations_service.repository.TechnicianRatingRepository;

@SpringBootTest
class ServiceOperationsServiceApplicationTests {

	@TestConfiguration
	static class TestMongoConfig {
		@Bean
		@Primary
		public MongoCustomConversions customConversions() {
			List<Converter<?, ?>> converters = new ArrayList<>();
			converters.add(new InstantToDateConverter());
			converters.add(new DateToInstantConverter());
			return new MongoCustomConversions(converters);
		}

		@Bean
		@Primary
		public MongoMappingContext mongoMappingContext(MongoCustomConversions customConversions) {
			MongoMappingContext context = new MongoMappingContext();
			context.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
			return context;
		}

		@WritingConverter
		public static class InstantToDateConverter implements Converter<Instant, Date> {
			@Override
			public Date convert(@NonNull Instant source) {
				return Date.from(source);
			}
		}

		@ReadingConverter
		public static class DateToInstantConverter implements Converter<Date, Instant> {
			@Override
			public Instant convert(@NonNull Date source) {
				return source.toInstant();
			}
		}
	}

	@MockBean
	private MongoDatabaseFactory mongoDatabaseFactory;

	@MockBean
	private ServiceCategoryRepository serviceCategoryRepository;

	@MockBean
	private ServiceItemRepository serviceItemRepository;

	@MockBean
	private InvoiceRepository invoiceRepository;

	@MockBean
	private ServiceRequestRepository serviceRequestRepository;

	@MockBean
	private TechnicianRatingRepository technicianRatingRepository;

	@Test
	void contextLoads() {
	}

}
