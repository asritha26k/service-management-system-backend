package com.app.service_operations_service.dto.billing;

import com.app.service_operations_service.model.enums.PaymentStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentUpdateRequest {

    @NotNull
    private PaymentStatus paymentStatus;

    @Size(max = 80)
    private String paymentMethod;
}
