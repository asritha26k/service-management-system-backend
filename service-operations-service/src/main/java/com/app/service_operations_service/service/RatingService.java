package com.app.service_operations_service.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.service_operations_service.client.TechnicianClient;
import com.app.service_operations_service.dto.rating.CreateRatingRequest;
import com.app.service_operations_service.dto.rating.RatingResponse;
import com.app.service_operations_service.dto.rating.TechnicianAverageRatingResponse;
import com.app.service_operations_service.exception.BadRequestException;
import com.app.service_operations_service.exception.NotFoundException;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.TechnicianRating;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.repository.ServiceRequestRepository;
import com.app.service_operations_service.repository.TechnicianRatingRepository;

@Service
@Transactional
public class RatingService {

    private static final Logger logger =
            LoggerFactory.getLogger(RatingService.class);

    private final TechnicianRatingRepository ratingRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final TechnicianClient technicianClient;

    public RatingService(TechnicianRatingRepository ratingRepository,
                         ServiceRequestRepository serviceRequestRepository,
                         TechnicianClient technicianClient) {
        this.ratingRepository = ratingRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.technicianClient = technicianClient;
    }

    public RatingResponse createRating(String customerId, CreateRatingRequest request) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(request.getServiceRequestId())
                .orElseThrow(() ->
                        new NotFoundException("Service request not found: " + request.getServiceRequestId()));

        if (!serviceRequest.getCustomerId().equals(customerId)) {
            throw new BadRequestException("You can only rate service requests that belong to you");
        }

        if (serviceRequest.getStatus() != RequestStatus.COMPLETED) {
            throw new BadRequestException("You can only rate completed service requests");
        }

        if (serviceRequest.getTechnicianId() == null) {
            throw new BadRequestException("Service request does not have a technician assigned");
        }

        if (ratingRepository.existsByServiceRequestId(request.getServiceRequestId())) {
            throw new BadRequestException("Rating already exists for this service request");
        }

        TechnicianRating rating = new TechnicianRating();
        rating.setTechnicianId(serviceRequest.getTechnicianId());
        rating.setCustomerId(customerId);
        rating.setServiceRequestId(request.getServiceRequestId());
        rating.setRating(request.getRating());
        rating.setComment(request.getComment());

        TechnicianRating saved = ratingRepository.save(rating);

        updateTechnicianAverageRating(serviceRequest.getTechnicianId());

        return toResponse(saved);
    }

    public List<RatingResponse> getRatingsByTechnician(String technicianId) {
        return ratingRepository.findByTechnicianId(technicianId).stream()
                .map(this::toResponse)
                .toList();
    }

    public TechnicianAverageRatingResponse getAverageRating(String technicianId) {
        List<TechnicianRating> ratings = ratingRepository.findByTechnicianId(technicianId);

        if (ratings.isEmpty()) {
            return new TechnicianAverageRatingResponse(technicianId, 0.0, 0L);
        }

        double average = ratings.stream()
                .mapToInt(TechnicianRating::getRating)
                .average()
                .orElse(0.0);

        return new TechnicianAverageRatingResponse(
                technicianId, average, (long) ratings.size());
    }

    private void updateTechnicianAverageRating(String technicianId) {
        try {
            TechnicianAverageRatingResponse avgRating = getAverageRating(technicianId);
            technicianClient.updateRating(technicianId, avgRating.getAverageRating());
        } catch (Exception ex) {
            // Log but do not fail rating creation
            logger.error(
                    "Failed to update technician rating for technicianId={}",
                    technicianId,
                    ex
            );
        }
    }

    private RatingResponse toResponse(TechnicianRating rating) {
        RatingResponse response = new RatingResponse();
        response.setId(rating.getId());
        response.setTechnicianId(rating.getTechnicianId());
        response.setCustomerId(rating.getCustomerId());
        response.setServiceRequestId(rating.getServiceRequestId());
        response.setRating(rating.getRating());
        response.setComment(rating.getComment());
        response.setCreatedAt(rating.getCreatedAt());
        return response;
    }
}
