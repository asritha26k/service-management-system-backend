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

    // Additional tests for improved coverage

    @Test
    void getDashboardSummary_ShouldHandleNullTechStats() {
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        List<Invoice> invoices = Arrays.asList(invoice);

        when(serviceRequestRepository.findAll()).thenReturn(requests);
        when(invoiceRepository.findAll()).thenReturn(invoices);
        when(technicianClient.getStats()).thenReturn(null);

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertEquals(0, response.getActiveTechnicians());
    }

    @Test
    void getDashboardSummary_ShouldHandleTechnicianClientException() {
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        List<Invoice> invoices = Arrays.asList(invoice);

        when(serviceRequestRepository.findAll()).thenReturn(requests);
        when(invoiceRepository.findAll()).thenReturn(invoices);
        when(technicianClient.getStats()).thenThrow(new RuntimeException("Service unavailable"));

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertEquals(0, response.getActiveTechnicians());
    }

    @Test
    void getDashboardSummary_ShouldCalculatePendingPayments() {
        ServiceRequest completedRequest = ServiceRequest.builder()
                .id("req-1")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.COMPLETED)
                .createdAt(Instant.now().minusSeconds(3600))
                .completedAt(Instant.now())
                .build();

        Invoice paidInvoice = Invoice.builder()
                .id("invoice-1")
                .customerId("customer-1")
                .totalAmount(new BigDecimal("100.00"))
                .paymentStatus(PaymentStatus.PAID)
                .createdAt(Instant.now())
                .build();

        Invoice pendingInvoice = Invoice.builder()
                .id("invoice-2")
                .customerId("customer-1")
                .totalAmount(new BigDecimal("50.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        List<ServiceRequest> requests = Arrays.asList(completedRequest);
        List<Invoice> invoices = Arrays.asList(paidInvoice, pendingInvoice);

        when(serviceRequestRepository.findAll()).thenReturn(requests);
        when(invoiceRepository.findAll()).thenReturn(invoices);
        when(technicianClient.getStats()).thenReturn(createTechStats());

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertEquals(1, response.getPendingPayments());
    }

    @Test
    void getDashboardSummary_ShouldCalculateMonthlyRevenue() {
        ServiceRequest request = ServiceRequest.builder()
                .id("req-1")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.COMPLETED)
                .createdAt(Instant.now().minusSeconds(3600))
                .completedAt(Instant.now())
                .build();

        Invoice invoice = Invoice.builder()
                .id("invoice-1")
                .customerId("customer-1")
                .totalAmount(new BigDecimal("110.00"))
                .paymentStatus(PaymentStatus.PAID)
                .createdAt(Instant.now())
                .build();

        List<ServiceRequest> requests = Arrays.asList(request);
        List<Invoice> invoices = Arrays.asList(invoice);

        when(serviceRequestRepository.findAll()).thenReturn(requests);
        when(invoiceRepository.findAll()).thenReturn(invoices);
        when(technicianClient.getStats()).thenReturn(createTechStats());

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertNotNull(response.getMonthlyRevenue());
    }

    @Test
    void getDashboardSummary_ShouldCountActiveAndCompletedRequests() {
        ServiceRequest activeRequest = ServiceRequest.builder()
                .id("req-1")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.ASSIGNED)
                .createdAt(Instant.now())
                .build();

        ServiceRequest completedRequest = ServiceRequest.builder()
                .id("req-2")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.COMPLETED)
                .createdAt(Instant.now().minusSeconds(3600))
                .completedAt(Instant.now())
                .build();

        List<ServiceRequest> requests = Arrays.asList(activeRequest, completedRequest);
        List<Invoice> invoices = Arrays.asList(invoice);

        when(serviceRequestRepository.findAll()).thenReturn(requests);
        when(invoiceRepository.findAll()).thenReturn(invoices);
        when(technicianClient.getStats()).thenReturn(createTechStats());

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertEquals(1, response.getTotalActiveRequests());
        assertEquals(1, response.getTotalCompletedRequests());
    }

    @Test
    void getCategoryStatistics_ShouldHandleEmptyRequests() {
        when(serviceRequestRepository.findAll()).thenReturn(List.of());
        when(serviceItemRepository.findAll()).thenReturn(List.of());
        when(serviceCategoryRepository.findAll()).thenReturn(List.of());
        when(serviceCategoryRepository.count()).thenReturn(0L);

        CategoryStatsResponse response = dashboardService.getCategoryStatistics();

        assertNotNull(response);
        assertEquals(0, response.getCategories().size());
        assertEquals(0L, response.getTotalCategories());
        assertEquals(0L, response.getTotalRequests());
    }

    @Test
    void getAverageResolutionTime_ShouldHandleNullDates() {
        ServiceRequest requestWithoutDates = ServiceRequest.builder()
                .id("req-1")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.COMPLETED)
                .createdAt(null)
                .completedAt(null)
                .build();

        List<ServiceRequest> completedRequests = Arrays.asList(requestWithoutDates);
        when(serviceRequestRepository.findByStatus(RequestStatus.COMPLETED)).thenReturn(completedRequests);

        ResolutionTimeResponse response = dashboardService.getAverageResolutionTime();

        assertNotNull(response);
        assertEquals(0.0, response.getAverageResolutionTimeHours());
    }

    @Test
    void getAverageResolutionTime_ShouldCalculateMultipleRequests() {
        ServiceRequest request1 = ServiceRequest.builder()
                .id("req-1")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.COMPLETED)
                .createdAt(Instant.now().minusSeconds(7200))
                .completedAt(Instant.now())
                .build();

        ServiceRequest request2 = ServiceRequest.builder()
                .id("req-2")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.COMPLETED)
                .createdAt(Instant.now().minusSeconds(3600))
                .completedAt(Instant.now())
                .build();

        List<ServiceRequest> completedRequests = Arrays.asList(request1, request2);
        when(serviceRequestRepository.findByStatus(RequestStatus.COMPLETED)).thenReturn(completedRequests);

        ResolutionTimeResponse response = dashboardService.getAverageResolutionTime();

        assertNotNull(response);
        assertTrue(response.getAverageResolutionTimeHours() > 0);
    }

    @Test
    void getTechnicianWorkload_ShouldConvertLongValues() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTechnicians", 10);
        stats.put("availableTechnicians", 7);
        stats.put("averageWorkloadRatio", 0.75);

        when(technicianClient.getStats()).thenReturn(stats);

        TechnicianWorkloadResponse response = dashboardService.getTechnicianWorkload();

        assertNotNull(response);
        assertEquals(10L, response.getTotalTechnicians());
    }

    @Test
    void getTechnicianWorkload_ShouldHandleNullStats() {
        when(technicianClient.getStats()).thenReturn(new HashMap<>());

        TechnicianWorkloadResponse response = dashboardService.getTechnicianWorkload();

        assertNotNull(response);
        assertEquals(0L, response.getTotalTechnicians());
        assertEquals(0.0, response.getAverageWorkloadRatio());
    }

    @Test
    void getDashboardSummary_ShouldCountRequestsByStatus() {
        ServiceRequest requestedStatus = ServiceRequest.builder()
                .id("req-1")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.REQUESTED)
                .createdAt(Instant.now())
                .build();

        ServiceRequest assignedStatus = ServiceRequest.builder()
                .id("req-2")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.ASSIGNED)
                .createdAt(Instant.now())
                .build();

        List<ServiceRequest> requests = Arrays.asList(requestedStatus, assignedStatus);
        List<Invoice> invoices = Arrays.asList();

        when(serviceRequestRepository.findAll()).thenReturn(requests);
        when(invoiceRepository.findAll()).thenReturn(invoices);
        when(technicianClient.getStats()).thenReturn(createTechStats());

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertNotNull(response.getServiceRequestsByStatus());
        assertTrue(response.getServiceRequestsByStatus().size() >= 2);
    }

    @Test
    void getDashboardSummary_ShouldGroupRequestsByService() {
        ServiceRequest request1 = ServiceRequest.builder()
                .id("req-1")
                .customerId("customer-1")
                .serviceId("service-1")
                .status(RequestStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        ServiceRequest request2 = ServiceRequest.builder()
                .id("req-2")
                .customerId("customer-1")
                .serviceId("service-2")
                .status(RequestStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();

        List<ServiceRequest> requests = Arrays.asList(request1, request2);
        List<Invoice> invoices = Arrays.asList();

        when(serviceRequestRepository.findAll()).thenReturn(requests);
        when(invoiceRepository.findAll()).thenReturn(invoices);
        when(technicianClient.getStats()).thenReturn(createTechStats());

        DashboardSummaryResponse response = dashboardService.getDashboardSummary();

        assertNotNull(response);
        assertNotNull(response.getServiceRequestsByCategory());
    }
}

