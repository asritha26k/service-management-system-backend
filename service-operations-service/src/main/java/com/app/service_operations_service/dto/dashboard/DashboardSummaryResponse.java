package com.app.service_operations_service.dto.dashboard;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DashboardSummaryResponse {

    private Map<String, Long> serviceRequestsByStatus;
    private Map<String, Long> serviceRequestsByCategory;
    private Long totalActiveRequests;
    private Long totalCompletedRequests;
    private Integer activeTechnicians;
    private Double averageResolutionTimeHours;
    private BigDecimal monthlyRevenue;
    private BigDecimal totalRevenue;
    private Long pendingPayments;
}
