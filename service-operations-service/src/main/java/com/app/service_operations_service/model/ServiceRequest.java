package com.app.service_operations_service.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.app.service_operations_service.model.enums.RequestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "service_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest {

    @Id
    private String id;
    private String requestNumber;
    private String customerId;
    private String serviceId;
    private String priority;
    @Builder.Default
    private RequestStatus status = RequestStatus.REQUESTED;

    @Field(targetType = FieldType.DATE_TIME)
    private Instant preferredDate;

    private String address;
    private String technicianId;

    @Field(targetType = FieldType.DATE_TIME)
    private Instant assignedAt;

    @Field(targetType = FieldType.DATE_TIME)
    private Instant acceptedAt;

    @Field(targetType = FieldType.DATE_TIME)
    private Instant completedAt;

    @Field(targetType = FieldType.DATE_TIME)
    @Builder.Default
    private Instant createdAt = Instant.now();

}
