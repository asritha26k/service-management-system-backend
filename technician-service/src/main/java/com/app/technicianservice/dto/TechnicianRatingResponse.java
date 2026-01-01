package com.app.technicianservice.dto;

import java.time.Instant;

import com.app.technicianservice.entity.TechnicianRating;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianRatingResponse {

    private String id;
    private String technicianId;
    private String customerId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;

    public TechnicianRatingResponse(TechnicianRating rating) {
        this.id = rating.getId();
        this.technicianId = rating.getTechnicianId();
        this.customerId = rating.getCustomerId();
        this.rating = rating.getRating();
        this.comment = rating.getComment();
        this.createdAt = rating.getCreatedAt();
        this.updatedAt = rating.getUpdatedAt();
    }
}
