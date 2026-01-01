package com.app.service_operations_service.dto.rating;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianAverageRatingResponse {

    private String technicianId;
    private Double averageRating;
    private Long totalRatings;
}
