package com.app.service_operations_service.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "service_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItem {

    @Id
    private String id;
    private String categoryId;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Duration estimatedDuration;
    private Integer slaHours;
    @Builder.Default
    private List<ServiceItemImage> images = new ArrayList<>();
    @Builder.Default
    private boolean isActive = true;
    @Builder.Default
    private Instant createdAt = Instant.now();

    // ===================== Nested Image class =====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceItemImage {
        private String url;
        private String alt;
    }
}
