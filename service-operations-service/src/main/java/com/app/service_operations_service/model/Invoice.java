package com.app.service_operations_service.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.app.service_operations_service.model.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    private String id;
    private String requestId;
    private String customerId;
    private BigDecimal serviceAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    private String paymentMethod;

    @Field(targetType = FieldType.DATE_TIME)
    private Instant paidAt;

    @Field(targetType = FieldType.DATE_TIME)
    @Builder.Default
    private Instant createdAt = Instant.now();

}
