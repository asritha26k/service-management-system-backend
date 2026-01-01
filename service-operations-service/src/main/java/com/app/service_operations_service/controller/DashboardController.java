package com.app.service_operations_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.service_operations_service.dto.dashboard.CategoryStatsResponse;
import com.app.service_operations_service.dto.dashboard.DashboardSummaryResponse;
import com.app.service_operations_service.dto.dashboard.ResolutionTimeResponse;
import com.app.service_operations_service.dto.dashboard.TechnicianWorkloadResponse;
import com.app.service_operations_service.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Get comprehensive dashboard summary
    // - Service request statistics by status and category
    // - Technician workload overview
    // - Average resolution time
    // - Monthly revenue summary
    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getDashboardSummary();
    }

    // Get technician workload report
    // Shows active technicians and their current workload
    @GetMapping("/technician-workload")
    public TechnicianWorkloadResponse getTechnicianWorkload() {
        return dashboardService.getTechnicianWorkload();
    }

    // Get service resolution metrics
    // Average time taken to complete service requests
    @GetMapping("/resolution-time")
    public ResolutionTimeResponse getResolutionTime() {
        return dashboardService.getAverageResolutionTime();
    }

    // Get service category statistics
    @GetMapping("/category-stats")
    public CategoryStatsResponse getCategoryStats() {
        return dashboardService.getCategoryStatistics();
    }
}
