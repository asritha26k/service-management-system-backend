package com.app.service_operations_service.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "service_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCategory {

    @Id
    private String id;
    private String name;
    private String description;
    @Builder.Default
    private boolean isActive = true;
    @Builder.Default
    private Instant createdAt = Instant.now();

}
