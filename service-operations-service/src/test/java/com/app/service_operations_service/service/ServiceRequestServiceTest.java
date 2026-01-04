package com.app.service_operations_service.service;

import com.app.service_operations_service.client.NotificationClient;
import com.app.service_operations_service.client.TechnicianClient;
import com.app.service_operations_service.client.dto.TechnicianProfileResponse;
import com.app.service_operations_service.dto.requests.*;
import com.app.service_operations_service.exception.NotFoundException;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
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
        when(requestRepository.findAll()).thenReturn(requests);

        List<ServiceRequestResponse> responses = serviceRequestService.getAll();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("req-1", responses.get(0).getId());
        verify(requestRepository, times(1)).findAll();
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

        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);

        ServiceRequestResponse response = serviceRequestService.create(request, "customer-1");

        assertNotNull(response);
        assertEquals("req-1", response.getId());
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
}
