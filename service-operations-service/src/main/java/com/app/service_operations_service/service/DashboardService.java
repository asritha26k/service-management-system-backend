package com.app.service_operations_service.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.app.service_operations_service.client.TechnicianClient;
import com.app.service_operations_service.dto.dashboard.CategoryStatsResponse;
import com.app.service_operations_service.dto.dashboard.DashboardSummaryResponse;
import com.app.service_operations_service.dto.dashboard.ResolutionTimeResponse;
import com.app.service_operations_service.dto.dashboard.TechnicianWorkloadResponse;
import com.app.service_operations_service.model.Invoice;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.enums.PaymentStatus;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.repository.InvoiceRepository;
import com.app.service_operations_service.repository.ServiceCategoryRepository;
import com.app.service_operations_service.repository.ServiceRequestRepository;

@Service
public class DashboardService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final InvoiceRepository invoiceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final TechnicianClient technicianClient;

    public DashboardService(
            ServiceRequestRepository serviceRequestRepository,
            InvoiceRepository invoiceRepository,
            ServiceCategoryRepository serviceCategoryRepository,
            TechnicianClient technicianClient
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.invoiceRepository = invoiceRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.technicianClient = technicianClient;
    }

    // Get comprehensive dashboard summary
    public DashboardSummaryResponse getDashboardSummary() {
        DashboardSummaryResponse response = new DashboardSummaryResponse();

        // Service requests by status
        List<ServiceRequest> allRequests = serviceRequestRepository.findAll();
        Map<String, Long> byStatus = allRequests.stream()
                .collect(Collectors.groupingBy(
                        sr -> sr.getStatus().name(),
                        Collectors.counting()
                ));
        response.setServiceRequestsByStatus(byStatus);

        // Total active and completed requests
        long activeRequests = allRequests.stream()
                .filter(sr -> sr.getStatus() != RequestStatus.COMPLETED &&
                        sr.getStatus() != RequestStatus.CANCELLED)
                .count();
        long completedRequests = allRequests.stream()
                .filter(sr -> sr.getStatus() == RequestStatus.COMPLETED)
                .count();
        response.setTotalActiveRequests(activeRequests);
        response.setTotalCompletedRequests(completedRequests);

        // Service requests by category
        Map<String, Long> byCategory = allRequests.stream()
                .filter(sr -> sr.getServiceId() != null)
                .collect(Collectors.groupingBy(
                        sr -> sr.getServiceId(),
                        Collectors.counting()
                ));
        response.setServiceRequestsByCategory(byCategory);

        // Average resolution time
        Double avgResolutionHours = calculateAverageResolutionTime();
        response.setAverageResolutionTimeHours(avgResolutionHours);

        // Revenue metrics
        List<Invoice> allInvoices = invoiceRepository.findAll();
        BigDecimal totalRevenue = allInvoices.stream()
                .filter(inv -> PaymentStatus.PAID.equals(inv.getPaymentStatus()))
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalRevenue(totalRevenue);

        // Monthly revenue (current month)
        LocalDate now = LocalDate.now();
        BigDecimal monthlyRevenue = allInvoices.stream()
                .filter(inv -> PaymentStatus.PAID.equals(inv.getPaymentStatus()))
                .filter(inv -> {
                    LocalDate invoiceDate = inv.getCreatedAt()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    return invoiceDate.getMonth() == now.getMonth() &&
                            invoiceDate.getYear() == now.getYear();
                })
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setMonthlyRevenue(monthlyRevenue);

        // Pending payments
        long pendingPayments = allInvoices.stream()
                .filter(inv -> PaymentStatus.PENDING.equals(inv.getPaymentStatus()))
                .count();
        response.setPendingPayments(pendingPayments);

        // Active technicians (from technician service)
        try {
            Map<String, Object> techStats = technicianClient.getStats();
            if (techStats != null) {
                // Use totalTechnicians as activeTechnicians for now
                Long total = getLongValue(techStats.get("totalTechnicians"));
                response.setActiveTechnicians(total != null ? total.intValue() : 0);
            } else {
                response.setActiveTechnicians(0);
            }
        } catch (Exception e) {
            response.setActiveTechnicians(0);
        }

        return response;
    }

    // Get technician workload statistics
    public TechnicianWorkloadResponse getTechnicianWorkload() {
        TechnicianWorkloadResponse response = new TechnicianWorkloadResponse();
        try {
            Map<String, Object> techStats = technicianClient.getStats();
            if (techStats != null) {
                response.setTotalTechnicians(getLongValue(techStats.get("totalTechnicians")));
                response.setAvailableTechnicians(getLongValue(techStats.get("availableTechnicians")));
                response.setAverageRating(getDoubleValue(techStats.get("averageRating")));
                response.setAverageWorkloadRatio(getDoubleValue(techStats.get("averageWorkloadRatio")));
            }
        } catch (Exception e) {
            // Return empty response with zeros
            response.setTotalTechnicians(0L);
            response.setAvailableTechnicians(0L);
            response.setAverageRating(0.0);
            response.setAverageWorkloadRatio(0.0);
        }
        return response;
    }

    // Calculate average resolution time for completed requests
    public ResolutionTimeResponse getAverageResolutionTime() {
        Double avgHours = calculateAverageResolutionTime();
        return new ResolutionTimeResponse(avgHours);
    }

    // Get service category statistics
    public CategoryStatsResponse getCategoryStatistics() {
        List<ServiceRequest> allRequests = serviceRequestRepository.findAll();
        
        // Group by service ID (which represents different services under categories)
        Map<String, Long> serviceStats = allRequests.stream()
                .filter(sr -> sr.getServiceId() != null)
                .collect(Collectors.groupingBy(
                        ServiceRequest::getServiceId,
                        Collectors.counting()
                ));

        CategoryStatsResponse response = new CategoryStatsResponse();
        response.setServiceStatistics(serviceStats);
        response.setTotalCategories(serviceCategoryRepository.count());
        
        return response;
    }

    // Helper: Calculate average resolution time
    private Double calculateAverageResolutionTime() {
        List<ServiceRequest> completedRequests = serviceRequestRepository
                .findByStatus(RequestStatus.COMPLETED);

        if (completedRequests.isEmpty()) {
            return 0.0;  // Return 0.0 instead of null
        }

        long totalHours = completedRequests.stream()
                .filter(sr -> sr.getCreatedAt() != null && sr.getCompletedAt() != null)
                .mapToLong(sr -> {
                    Duration duration = Duration.between(sr.getCreatedAt(), sr.getCompletedAt());
                    return duration.toHours();
                })
                .sum();

        long validCount = completedRequests.stream()
                .filter(sr -> sr.getCreatedAt() != null && sr.getCompletedAt() != null)
                .count();

        return validCount > 0 ? (double) totalHours / validCount : 0.0;
    }

    // Helper: Safely convert Object to Long
    private Long getLongValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    // Helper: Safely convert Object to Double
    private Double getDoubleValue(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }
}
