package com.app.service_operations_service.dto.requests;

import java.time.Instant;

import com.app.service_operations_service.model.enums.RequestStatus;

import lombok.Data;

@Data
public class ServiceRequestWithTechnicianResponse {

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
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant completedAt;
    private Instant createdAt;
    private TechnicianDetails technicianDetails;

    @Data
    public static class TechnicianDetails {
        private String id;
        private String email;
        private String name;
        private String phone;
        private String specialization;
        private Integer experience;
        private Double rating;
    }
}
