package com.app.service_operations_service.dto.requests;

import java.util.Map;

import com.app.service_operations_service.model.enums.RequestStatus;

import lombok.Data;

@Data
public class ServiceRequestStatsResponse {
    private Map<RequestStatus, Long> byStatus;
    private long total;
}
