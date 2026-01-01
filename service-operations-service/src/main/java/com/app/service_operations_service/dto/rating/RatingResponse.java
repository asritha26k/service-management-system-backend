package com.app.service_operations_service.dto.rating;

import java.time.Instant;

import lombok.Data;

@Data
public class RatingResponse {

    private String id;
    private String technicianId;
    private String customerId;
    private String serviceRequestId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
