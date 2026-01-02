package com.app.service_operations_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.service_operations_service.dto.rating.CreateRatingRequest;
import com.app.service_operations_service.dto.rating.RatingResponse;
import com.app.service_operations_service.dto.rating.TechnicianAverageRatingResponse;
import com.app.service_operations_service.dto.IdMessageResponse;
import com.app.service_operations_service.service.RatingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdMessageResponse createRating(
            @RequestHeader("X-User-Id") String customerId,
            @Valid @RequestBody CreateRatingRequest request) {
        RatingResponse response = ratingService.createRating(customerId, request);
        return new IdMessageResponse(response.getId(), "Rating submitted successfully");
    }

    @GetMapping("/technician/{technicianId}")
    public List<RatingResponse> getRatingsByTechnician(@PathVariable("technicianId") String technicianId) {
        return ratingService.getRatingsByTechnician(technicianId);
    }

    @GetMapping("/technician/{technicianId}/average")
    public TechnicianAverageRatingResponse getAverageRating(@PathVariable("technicianId") String technicianId) {
        return ratingService.getAverageRating(technicianId);
    }
}
