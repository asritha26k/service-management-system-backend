package com.app.service_operations_service.service;

import com.app.service_operations_service.client.TechnicianClient;
import com.app.service_operations_service.client.dto.TechnicianProfileResponse;
import com.app.service_operations_service.dto.rating.*;
import com.app.service_operations_service.exception.BadRequestException;
import com.app.service_operations_service.exception.NotFoundException;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.TechnicianRating;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.repository.ServiceRequestRepository;
import com.app.service_operations_service.repository.TechnicianRatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private TechnicianRatingRepository ratingRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private TechnicianClient technicianClient;

    @InjectMocks
    private RatingService ratingService;

    private ServiceRequest serviceRequest;
    private TechnicianRating rating;

    @BeforeEach
    void setUp() {
        serviceRequest = ServiceRequest.builder()
                .id("req-1")
                .customerId("customer-1")
                .technicianId("tech-1")
                .status(RequestStatus.COMPLETED)
                .build();

        rating = TechnicianRating.builder()
                .id("rating-1")
                .technicianId("tech-1")
                .customerId("customer-1")
                .serviceRequestId("req-1")
                .rating(5)
                .comment("Excellent service!")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void createRating_ShouldCreateRating() {
        CreateRatingRequest request = new CreateRatingRequest();
        request.setServiceRequestId("req-1");
        request.setRating(5);
        request.setComment("Excellent service!");

        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(ratingRepository.existsByServiceRequestId("req-1")).thenReturn(false);
        when(ratingRepository.save(any(TechnicianRating.class))).thenReturn(rating);
        doNothing().when(technicianClient).updateRating(anyString(), anyDouble());

        RatingResponse response = ratingService.createRating("customer-1", request);

        assertNotNull(response);
        assertEquals("rating-1", response.getId());
        assertEquals(5, response.getRating());
        verify(ratingRepository, times(1)).save(any(TechnicianRating.class));
    }

    @Test
    void createRating_ShouldThrowNotFoundException_WhenRequestNotFound() {
        CreateRatingRequest request = new CreateRatingRequest();
        request.setServiceRequestId("invalid-req");
        request.setRating(5);

        when(serviceRequestRepository.findById("invalid-req")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ratingService.createRating("customer-1", request));
        verify(ratingRepository, never()).save(any(TechnicianRating.class));
    }

    @Test
    void createRating_ShouldThrowBadRequest_WhenCustomerNotOwner() {
        CreateRatingRequest request = new CreateRatingRequest();
        request.setServiceRequestId("req-1");
        request.setRating(5);

        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> ratingService.createRating("different-customer", request));
    }

    @Test
    void createRating_ShouldThrowBadRequest_WhenRequestNotCompleted() {
        CreateRatingRequest request = new CreateRatingRequest();
        request.setServiceRequestId("req-1");
        request.setRating(5);

        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> ratingService.createRating("customer-1", request));
    }

    @Test
    void createRating_ShouldThrowBadRequest_WhenRatingAlreadyExists() {
        CreateRatingRequest request = new CreateRatingRequest();
        request.setServiceRequestId("req-1");
        request.setRating(5);

        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(ratingRepository.existsByServiceRequestId("req-1")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> ratingService.createRating("customer-1", request));
        verify(ratingRepository, never()).save(any(TechnicianRating.class));
    }

    @Test
    void getRatingsByTechnician_ShouldReturnRatings() {
        List<TechnicianRating> ratings = Arrays.asList(rating);
        when(ratingRepository.findByTechnicianId("tech-1")).thenReturn(ratings);

        List<RatingResponse> responses = ratingService.getRatingsByTechnician("tech-1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("tech-1", responses.get(0).getTechnicianId());
        verify(ratingRepository, times(1)).findByTechnicianId("tech-1");
    }

    @Test
    void getAverageRating_ShouldReturnAverage() {
        List<TechnicianRating> ratings = Arrays.asList(rating);
        when(ratingRepository.findByTechnicianId("tech-1")).thenReturn(ratings);

        TechnicianAverageRatingResponse response = ratingService.getAverageRating("tech-1");

        assertNotNull(response);
        assertEquals("tech-1", response.getTechnicianId());
        assertEquals(5.0, response.getAverageRating());
        assertEquals(1L, response.getTotalRatings());
        verify(ratingRepository, times(1)).findByTechnicianId("tech-1");
    }

    @Test
    void getAverageRating_ShouldReturnZero_WhenNoRatings() {
        when(ratingRepository.findByTechnicianId("tech-1")).thenReturn(List.of());

        TechnicianAverageRatingResponse response = ratingService.getAverageRating("tech-1");

        assertNotNull(response);
        assertEquals("tech-1", response.getTechnicianId());
        assertEquals(0.0, response.getAverageRating());
        assertEquals(0L, response.getTotalRatings());
    }
}

