package com.app.service_operations_service.dto.requests;

import java.time.Instant;

import com.app.service_operations_service.model.enums.RequestStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class ServiceRequestResponse {
    private String id;
    private String requestNumber;
    private String customerId;
    private String serviceId;
    private String serviceName;
    private String priority;
    private RequestStatus status;
    private Instant preferredDate;
    private String address;
    private String technicianId;
    private String technicianName;
    private String technicianPhone;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant completedAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}
