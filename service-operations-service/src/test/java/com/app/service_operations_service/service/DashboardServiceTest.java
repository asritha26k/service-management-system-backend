package com.app.service_operations_service.service;

import com.app.service_operations_service.client.TechnicianClient;
import com.app.service_operations_service.dto.dashboard.*;
import com.app.service_operations_service.model.Invoice;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.enums.PaymentStatus;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.repository.InvoiceRepository;
import com.app.service_operations_service.repository.ServiceCategoryRepository;
import com.app.service_operations_service.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ServiceCategoryRepository serviceCategoryRepository;

    @Mock
    private com.app.service_operations_service.repository.ServiceItemRepository serviceItemRepository;

    @Mock
    private TechnicianClient technicianClient;

    @InjectMocks
    private DashboardService dashboardService;

    private ServiceRequest serviceRequest;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        serviceRequest = ServiceRequest.builder()
                .id("req-1")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.COMPLETED)
                .createdAt(Instant.now().minusSeconds(3600))
                .completedAt(Instant.now())
                .build();

        invoice = Invoice.builder()
                .id("invoice-1")
                .customerId("customer-1")
                .totalAmount(new BigDecimal("110.00"))
                .paymentStatus(PaymentStatus.PAID)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getDashboardSummary_ShouldReturnSummary() {
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        List<Invoice> invoices = Arrays.asList(invoice);

        when(serviceRequestRepository.findAll()).thenReturn(requests);
        when(invoiceRepository.findAll()).thenReturn(invoices);
        when(technicianClient.getStats()).thenReturn(createTechStats());

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertNotNull(response.getServiceRequestsByStatus());
        assertNotNull(response.getTotalRevenue());
        verify(serviceRequestRepository, times(1)).findAll();
        verify(invoiceRepository, times(1)).findAll();
    }

    @Test
    void getTechnicianWorkload_ShouldReturnWorkload() {
        when(technicianClient.getStats()).thenReturn(createTechStats());

        TechnicianWorkloadResponse response = dashboardService.getTechnicianWorkload();

        assertNotNull(response);
        verify(technicianClient, times(1)).getStats();
    }

    @Test
    void getTechnicianWorkload_ShouldHandleException() {
        when(technicianClient.getStats()).thenThrow(new RuntimeException("Service unavailable"));

        TechnicianWorkloadResponse response = dashboardService.getTechnicianWorkload();

        assertNotNull(response);
        assertEquals(0L, response.getTotalTechnicians());
    }

    @Test
    void getAverageResolutionTime_ShouldReturnAverageTime() {
        List<ServiceRequest> completedRequests = Arrays.asList(serviceRequest);
        when(serviceRequestRepository.findByStatus(RequestStatus.COMPLETED)).thenReturn(completedRequests);

        ResolutionTimeResponse response = dashboardService.getAverageResolutionTime();

        assertNotNull(response);
        assertNotNull(response.getAverageResolutionTimeHours());
        verify(serviceRequestRepository, times(1)).findByStatus(RequestStatus.COMPLETED);
    }

    @Test
    void getAverageResolutionTime_ShouldReturnZero_WhenNoCompletedRequests() {
        when(serviceRequestRepository.findByStatus(RequestStatus.COMPLETED)).thenReturn(List.of());

        ResolutionTimeResponse response = dashboardService.getAverageResolutionTime();

        assertNotNull(response);
        assertEquals(0.0, response.getAverageResolutionTimeHours());
    }

    @Test
    void getCategoryStatistics_ShouldReturnCategoryStats() {
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        when(serviceRequestRepository.findAll()).thenReturn(requests);
        when(serviceItemRepository.findAll()).thenReturn(List.of());
        when(serviceCategoryRepository.findAll()).thenReturn(List.of());
        when(serviceCategoryRepository.count()).thenReturn(5L);

        CategoryStatsResponse response = dashboardService.getCategoryStatistics();

        assertNotNull(response);
        assertNotNull(response.getCategories());
        assertEquals(5L, response.getTotalCategories());
        verify(serviceRequestRepository, times(1)).findAll();
        verify(serviceCategoryRepository, times(1)).count();
    }

    private Map<String, Object> createTechStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTechnicians", 10L);
        stats.put("availableTechnicians", 7L);
        stats.put("averageRating", 4.5);
        stats.put("averageWorkloadRatio", 0.75);
        return stats;
    }
}

