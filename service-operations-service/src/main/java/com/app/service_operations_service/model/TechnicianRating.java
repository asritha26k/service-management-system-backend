package com.app.service_operations_service.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "technician_ratings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianRating {

    @Id
    private String id;
    private String technicianId;
    private String customerId;
    private String serviceRequestId;
    private Integer rating; // 1 to 5
    private String comment;
    @Builder.Default
    private Instant createdAt = Instant.now();

}
