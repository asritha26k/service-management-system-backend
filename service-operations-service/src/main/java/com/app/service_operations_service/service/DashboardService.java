package com.app.service_operations_service.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.app.service_operations_service.client.TechnicianClient;
import com.app.service_operations_service.dto.dashboard.CategoryStatistics;
import com.app.service_operations_service.dto.dashboard.CategoryStatsResponse;
import com.app.service_operations_service.dto.dashboard.DashboardSummaryResponse;
import com.app.service_operations_service.dto.dashboard.ResolutionTimeResponse;
import com.app.service_operations_service.dto.dashboard.TechnicianWorkloadResponse;
import com.app.service_operations_service.model.Invoice;
import com.app.service_operations_service.model.ServiceCategory;
import com.app.service_operations_service.model.ServiceItem;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.enums.PaymentStatus;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.repository.InvoiceRepository;
import com.app.service_operations_service.repository.ServiceCategoryRepository;
import com.app.service_operations_service.repository.ServiceItemRepository;
import com.app.service_operations_service.repository.ServiceRequestRepository;

@Service
public class DashboardService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final InvoiceRepository invoiceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final TechnicianClient technicianClient;

    public DashboardService(
            ServiceRequestRepository serviceRequestRepository,
            InvoiceRepository invoiceRepository,
            ServiceCategoryRepository serviceCategoryRepository,
            ServiceItemRepository serviceItemRepository,
            TechnicianClient technicianClient
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.invoiceRepository = invoiceRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.serviceItemRepository = serviceItemRepository;
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
                response.setAverageWorkloadRatio(getDoubleValue(techStats.get("averageWorkloadRatio")));
            }
        } catch (Exception e) {
            // Return empty response with zeros
            response.setTotalTechnicians(0L);
            response.setAvailableTechnicians(0L);
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
        
        // Group by service ID
        Map<String, Long> serviceRequestCounts = allRequests.stream()
                .filter(sr -> sr.getServiceId() != null)
                .collect(Collectors.groupingBy(
                        ServiceRequest::getServiceId,
                        Collectors.counting()
                ));

        // Get all service items and categories
        List<ServiceItem> allServiceItems = serviceItemRepository.findAll();
        List<ServiceCategory> allCategories = serviceCategoryRepository.findAll();
        
        // Create maps for quick lookup
        Map<String, ServiceItem> serviceItemMap = allServiceItems.stream()
                .collect(Collectors.toMap(ServiceItem::getId, item -> item));
        Map<String, ServiceCategory> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(ServiceCategory::getId, cat -> cat));

        // Group services by category
        Map<String, List<CategoryStatistics.ServiceInfo>> servicesByCategory = new java.util.HashMap<>();
        
        for (Map.Entry<String, Long> entry : serviceRequestCounts.entrySet()) {
            String serviceId = entry.getKey();
            Long count = entry.getValue();
            
            ServiceItem serviceItem = serviceItemMap.get(serviceId);
            if (serviceItem != null && serviceItem.getCategoryId() != null) {
                String categoryId = serviceItem.getCategoryId();
                
                CategoryStatistics.ServiceInfo serviceInfo = CategoryStatistics.ServiceInfo.builder()
                        .serviceId(serviceId)
                        .serviceName(serviceItem.getName())
                        .requestCount(count)
                        .build();
                
                servicesByCategory
                        .computeIfAbsent(categoryId, k -> new ArrayList<>())
                        .add(serviceInfo);
            }
        }

        // Build category statistics
        List<CategoryStatistics> categoryStatsList = new ArrayList<>();
        
        for (Map.Entry<String, List<CategoryStatistics.ServiceInfo>> entry : servicesByCategory.entrySet()) {
            String categoryId = entry.getKey();
            List<CategoryStatistics.ServiceInfo> services = entry.getValue();
            
            ServiceCategory category = categoryMap.get(categoryId);
            String categoryName = (category != null) ? category.getName() : "Unknown";
            
            Long totalRequestsInCategory = services.stream()
                    .mapToLong(CategoryStatistics.ServiceInfo::getRequestCount)
                    .sum();
            
            CategoryStatistics categoryStats = CategoryStatistics.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .totalRequests(totalRequestsInCategory)
                    .services(services)
                    .build();
            
            categoryStatsList.add(categoryStats);
        }

        // Sort by total requests descending
        categoryStatsList.sort((a, b) -> b.getTotalRequests().compareTo(a.getTotalRequests()));

        Long totalRequests = allRequests.stream()
                .filter(sr -> sr.getServiceId() != null)
                .count();

        return CategoryStatsResponse.builder()
                .categories(categoryStatsList)
                .totalCategories(serviceCategoryRepository.count())
                .totalRequests(totalRequests)
                .build();
    }

    // Helper: Calculate average resolution time
    private Double calculateAverageResolutionTime() {
        List<ServiceRequest> completedRequests = serviceRequestRepository
                .findByStatus(RequestStatus.COMPLETED);

        if (completedRequests.isEmpty()) {
            return 0.0;  // Return 0.0 instead of null
        }

        double totalMinutes = completedRequests.stream()
                .filter(sr -> sr.getCreatedAt() != null && sr.getCompletedAt() != null)
                .mapToDouble(sr -> {
                    Duration duration = Duration.between(sr.getCreatedAt(), sr.getCompletedAt());
                    return duration.toMinutes(); // Get total minutes with precision
                })
                .sum();

        long validCount = completedRequests.stream()
                .filter(sr -> sr.getCreatedAt() != null && sr.getCompletedAt() != null)
                .count();

        if (validCount > 0) {
            double averageMinutes = totalMinutes / validCount;
            return Math.round((averageMinutes / 60.0) * 100.0) / 100.0; // Convert to hours and round to 2 decimals
        }
        
        return 0.0;
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
