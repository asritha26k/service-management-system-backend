package com.app.service_operations_service.dto.catalog;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UpdateServiceItemRequest {

    @NotBlank
    private String name;

    @Size(max = 800)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal basePrice;

    @NotNull
    @Positive
    private Long estimatedDurationMinutes;

    @NotNull
    @Positive
    private Integer slaHours;

    private Boolean active;

    private List<ServiceItemImagePayload> images;

    @Data
    public static class ServiceItemImagePayload {
        @NotBlank
        private String url;
        @Size(max = 200)
        private String alt;
    }
}
