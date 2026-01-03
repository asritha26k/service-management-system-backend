package com.app.service_operations_service.controller;

import com.app.service_operations_service.dto.IdMessageResponse;
import com.app.service_operations_service.dto.rating.*;
import com.app.service_operations_service.service.RatingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = RatingController.class, excludeAutoConfiguration = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    EurekaClientAutoConfiguration.class
})
@TestPropertySource(properties = {
    "logging.level.root=INFO",
    "logging.level.com.app.service_operations_service=INFO",
    "spring.application.name=service-operations-service-test",
    "server.port=0"
})
class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RatingService ratingService;

    @Autowired
    private ObjectMapper objectMapper;

    private RatingResponse ratingResponse;
    private TechnicianAverageRatingResponse averageRatingResponse;

    @BeforeEach
    void setUp() {
        ratingResponse = new RatingResponse();
        ratingResponse.setId("rating-1");
        ratingResponse.setTechnicianId("tech-1");
        ratingResponse.setCustomerId("customer-1");
        ratingResponse.setServiceRequestId("req-1");
        ratingResponse.setRating(5);
        ratingResponse.setComment("Excellent service!");
        ratingResponse.setCreatedAt(Instant.now());

        averageRatingResponse = new TechnicianAverageRatingResponse();
        averageRatingResponse.setTechnicianId("tech-1");
        averageRatingResponse.setAverageRating(4.5);
        averageRatingResponse.setTotalRatings(10L);
    }

    @Test
    void createRating_ShouldReturnCreated() throws Exception {
        CreateRatingRequest request = new CreateRatingRequest();
        request.setServiceRequestId("req-1");
        request.setRating(5);
        request.setComment("Excellent service!");

        when(ratingService.createRating(eq("customer-1"), any(CreateRatingRequest.class)))
                .thenReturn(ratingResponse);

        mockMvc.perform(post("/api/ratings")
                        .header("X-User-Id", "customer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("rating-1"))
                .andExpect(jsonPath("$.message").value("Rating submitted successfully"));
    }

    @Test
    void createRating_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        CreateRatingRequest request = new CreateRatingRequest();
        request.setServiceRequestId(""); // Invalid: blank serviceRequestId
        request.setRating(5);

        mockMvc.perform(post("/api/ratings")
                        .header("X-User-Id", "customer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRatingsByTechnician_ShouldReturnOk() throws Exception {
        List<RatingResponse> ratings = Arrays.asList(ratingResponse);
        when(ratingService.getRatingsByTechnician("tech-1")).thenReturn(ratings);

        mockMvc.perform(get("/api/ratings/technician/tech-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].technicianId").value("tech-1"))
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    @Test
    void getAverageRating_ShouldReturnOk() throws Exception {
        when(ratingService.getAverageRating("tech-1")).thenReturn(averageRatingResponse);

        mockMvc.perform(get("/api/ratings/technician/tech-1/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technicianId").value("tech-1"))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.totalRatings").value(10));
    }
}

