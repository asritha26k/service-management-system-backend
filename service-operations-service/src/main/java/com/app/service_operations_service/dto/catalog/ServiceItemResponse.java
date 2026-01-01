package com.app.service_operations_service.dto.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class ServiceItemResponse {
    private String id;
    private String categoryId;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Long estimatedDurationMinutes;
    private Integer slaHours;
    private List<ServiceItemImagePayload> images;
    private boolean active;
    private Instant createdAt;

    @Data
    public static class ServiceItemImagePayload {
        private String url;
        private String alt;
    }
}
