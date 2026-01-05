package com.app.service_operations_service.service;

import com.app.service_operations_service.client.NotificationClient;
import com.app.service_operations_service.client.TechnicianClient;
import com.app.service_operations_service.client.dto.TechnicianProfileResponse;
import com.app.service_operations_service.dto.PagedResponse;
import com.app.service_operations_service.dto.requests.*;
import com.app.service_operations_service.exception.NotFoundException;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock
    private ServiceRequestRepository requestRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private TechnicianClient technicianClient;

    @InjectMocks
    private ServiceRequestService serviceRequestService;

    private ServiceRequest serviceRequest;

    @BeforeEach
    void setUp() {
        serviceRequest = ServiceRequest.builder()
                .id("req-1")
                .requestNumber("REQ-12345678")
                .customerId("customer-1")
                .serviceId("service-1")
                .priority("HIGH")
                .status(RequestStatus.REQUESTED)
                .preferredDate(Instant.now().plusSeconds(3600))
                .address("123 Main St")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getAll_ShouldReturnAllRequests() {
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ServiceRequest> page = new PageImpl<>(requests, pageable, requests.size());

        when(requestRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResponse<ServiceRequestResponse> response = serviceRequestService.getAll(pageable);

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertEquals(1, response.getContent().size());
        assertEquals("req-1", response.getContent().get(0).getId());
        assertEquals(0, response.getPageNumber());
        assertEquals(20, response.getPageSize());
        assertEquals(1, response.getTotalElements());
        verify(requestRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void getByStatus_ShouldReturnRequestsByStatus() {
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        when(requestRepository.findByStatus(RequestStatus.REQUESTED)).thenReturn(requests);

        List<ServiceRequestResponse> responses = serviceRequestService.getByStatus("REQUESTED");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(RequestStatus.REQUESTED, responses.get(0).getStatus());
        verify(requestRepository, times(1)).findByStatus(RequestStatus.REQUESTED);
    }

    @Test
    void create_ShouldCreateServiceRequest() {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setServiceId("service-1");
        request.setPriority("HIGH");
        request.setPreferredDate(Instant.now().plusSeconds(3600));
        request.setAddress("123 Main St");

        when(requestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> {
            ServiceRequest savedRequest = invocation.getArgument(0);
            savedRequest.setId("req-1"); // Simulate auto-generated ID
            savedRequest.setStatus(RequestStatus.REQUESTED); // Simulate default status
            savedRequest.setCreatedAt(Instant.now()); // Simulate timestamp
            return savedRequest;
        });

        ServiceRequestResponse response = serviceRequestService.create(request, "customer-1");

        assertNotNull(response);
        assertEquals("req-1", response.getId());
        assertEquals(RequestStatus.REQUESTED, response.getStatus());
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void getById_ShouldReturnRequest() {
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        ServiceRequestResponse response = serviceRequestService.getById("req-1");

        assertNotNull(response);
        assertEquals("req-1", response.getId());
        verify(requestRepository, times(1)).findById("req-1");
    }

    @Test
    void getById_ShouldThrowNotFoundException_WhenNotFound() {
        when(requestRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> serviceRequestService.getById("invalid-id"));
    }

    @Test
    void getByCustomer_ShouldReturnCustomerRequests() {
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        when(requestRepository.findByCustomerId("customer-1")).thenReturn(requests);

        List<ServiceRequestResponse> responses = serviceRequestService.getByCustomer("customer-1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("customer-1", responses.get(0).getCustomerId());
        verify(requestRepository, times(1)).findByCustomerId("customer-1");
    }

    @Test
    void assign_ShouldAssignTechnician() {
        AssignRequest assignRequest = new AssignRequest();
        assignRequest.setTechnicianId("tech-1");

        TechnicianProfileResponse technician = new TechnicianProfileResponse();
        technician.setId("tech-1");
        technician.setUserId("user-123");

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        when(technicianClient.getTechnician("tech-1")).thenReturn(technician);

        serviceRequestService.assign("req-1", assignRequest);

        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void updateStatus_ShouldUpdateStatus() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("ASSIGNED");

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);

        serviceRequestService.updateStatus("req-1", request);

        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void stats_ShouldReturnStats() {
        when(requestRepository.count()).thenReturn(1L);
        when(requestRepository.countByStatus(RequestStatus.COMPLETED)).thenReturn(0L);
        when(requestRepository.countByStatus(RequestStatus.REQUESTED)).thenReturn(1L);
        when(requestRepository.countByStatus(RequestStatus.IN_PROGRESS)).thenReturn(0L);

        ServiceRequestStatsResponse response = serviceRequestService.stats();

        assertNotNull(response);
        assertNotNull(response.getByStatus());
        assertEquals(1, response.getTotal());
        verify(requestRepository, times(1)).count();
    }

    @Test
    void acceptWork_ShouldAcceptWork() {
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);

        serviceRequestService.acceptWork("req-1", "tech-user-1");

        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void rejectWork_ShouldRejectWork() {
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);

        serviceRequestService.rejectWork("req-1", "tech-user-1", "Reason");

        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void completeByTechnician_ShouldCompleteRequest() {
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);

        ServiceRequestResponse response = serviceRequestService.completeByTechnician("req-1", "tech-user-1");

        assertNotNull(response);
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void cancel_ShouldCancelRequest() {
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);

        serviceRequestService.cancel("req-1", "customer-1");

        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void reschedule_ShouldRescheduleRequest() {
        RescheduleServiceRequest request = new RescheduleServiceRequest();
        request.setPreferredDate(Instant.now().plusSeconds(7200));

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);

        serviceRequestService.reschedule("req-1", "customer-1", request);

        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void getByCustomerWithTechnicianDetails_ShouldReturnRequestsWithTechnicianDetails() {
        serviceRequest.setTechnicianId("tech-1");
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        when(requestRepository.findByCustomerId("customer-1")).thenReturn(requests);

        List<ServiceRequestWithTechnicianResponse> responses = serviceRequestService
                .getByCustomerWithTechnicianDetails("customer-1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("customer-1", responses.get(0).getCustomerId());
        assertEquals("tech-1", responses.get(0).getTechnicianId());
        verify(requestRepository, times(1)).findByCustomerId("customer-1");
    }

    @Test
    void getByTechnicianUserIdWithCustomerDetails_ShouldReturnRequestsWithCustomerDetails() {
        TechnicianProfileResponse technician = new TechnicianProfileResponse();
        technician.setId("tech-1");
        technician.setUserId("user-123");

        serviceRequest.setTechnicianId("tech-1");
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);

        when(technicianClient.getTechnicianByUserId("user-123")).thenReturn(technician);
        when(requestRepository.findByTechnicianId("tech-1")).thenReturn(requests);

        List<ServiceRequestWithCustomerResponse> responses = serviceRequestService
                .getByTechnicianUserIdWithCustomerDetails("user-123");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("customer-1", responses.get(0).getCustomerId());
        verify(technicianClient, times(1)).getTechnicianByUserId("user-123");
        verify(requestRepository, times(1)).findByTechnicianId("tech-1");
    }

    @Test
    void getByTechnicianUserIdWithCustomerDetails_ShouldReturnEmptyList_WhenTechnicianNotFound() {
        when(technicianClient.getTechnicianByUserId("user-999")).thenReturn(null);

        List<ServiceRequestWithCustomerResponse> responses = serviceRequestService
                .getByTechnicianUserIdWithCustomerDetails("user-999");

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(technicianClient, times(1)).getTechnicianByUserId("user-999");
        verify(requestRepository, never()).findByTechnicianId(anyString());
    }

    @Test
    void getByTechnicianUserId_ShouldReturnRequestsForTechnician() {
        TechnicianProfileResponse technician = new TechnicianProfileResponse();
        technician.setId("tech-1");
        technician.setUserId("user-123");

        serviceRequest.setTechnicianId("tech-1");
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);

        when(technicianClient.getTechnicianByUserId("user-123")).thenReturn(technician);
        when(requestRepository.findByTechnicianId("tech-1")).thenReturn(requests);

        List<ServiceRequestResponse> responses = serviceRequestService.getByTechnicianUserId("user-123");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("req-1", responses.get(0).getId());
        verify(technicianClient, times(1)).getTechnicianByUserId("user-123");
        verify(requestRepository, times(1)).findByTechnicianId("tech-1");
    }

    @Test
    void getByTechnicianUserId_ShouldReturnEmptyList_WhenTechnicianNotFound() {
        when(technicianClient.getTechnicianByUserId("user-999")).thenReturn(null);

        List<ServiceRequestResponse> responses = serviceRequestService.getByTechnicianUserId("user-999");

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(technicianClient, times(1)).getTechnicianByUserId("user-999");
        verify(requestRepository, never()).findByTechnicianId(anyString());
    }

    @Test
    void completeByTechnician_ShouldThrowException_WhenRequestNotFound() {
        when(requestRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> serviceRequestService.completeByTechnician("invalid-id", "tech-user-1"));

        verify(requestRepository, never()).save(any(ServiceRequest.class));
    }
}
