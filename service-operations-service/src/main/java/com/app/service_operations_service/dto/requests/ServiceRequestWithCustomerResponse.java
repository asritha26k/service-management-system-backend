package com.app.service_operations_service.dto.requests;

import java.time.Instant;

import com.app.service_operations_service.model.enums.RequestStatus;

import lombok.Data;

@Data
public class ServiceRequestWithCustomerResponse {

    private String id;
    private String requestNumber;
    private String customerId;
    private String serviceId;
    private String priority;
    private RequestStatus status;
    private Instant preferredDate;
    private String address;
    private String technicianId;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant completedAt;
    private Instant createdAt;
    private CustomerDetails customerDetails;

    @Data
    public static class CustomerDetails {
        private String id;
        private String name;
        private String email;
        private String phone;
        private String address;
    }
}
