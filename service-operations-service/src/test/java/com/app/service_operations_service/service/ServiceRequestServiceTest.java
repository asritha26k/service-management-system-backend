package com.app.service_operations_service.service;

import com.app.service_operations_service.client.IdentityClient;
import com.app.service_operations_service.client.NotificationClient;
import com.app.service_operations_service.client.TechnicianClient;
import com.app.service_operations_service.client.dto.CustomerSummary;
import com.app.service_operations_service.client.dto.TechnicianProfileResponse;
import com.app.service_operations_service.dto.requests.*;
import com.app.service_operations_service.exception.BadRequestException;
import com.app.service_operations_service.exception.NotFoundException;
import com.app.service_operations_service.exception.UnauthorizedException;
import com.app.service_operations_service.model.ServiceItem;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.repository.ServiceItemRepository;
import com.app.service_operations_service.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
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
    private ServiceItemRepository itemRepository;

    @Mock
    private IdentityClient identityClient;

    @Mock
    private TechnicianClient technicianClient;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private BillingService billingService;

    @InjectMocks
    private ServiceRequestService serviceRequestService;

    private ServiceRequest serviceRequest;
    private ServiceItem serviceItem;
    private CustomerSummary customerSummary;
    private TechnicianProfileResponse technicianProfile;

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

        serviceItem = ServiceItem.builder()
                .id("service-1")
                .name("Test Service")
                .basePrice(new BigDecimal("100.00"))
                .build();

        customerSummary = new CustomerSummary();
        customerSummary.setId("customer-1");
        customerSummary.setActive(true);

        technicianProfile = new TechnicianProfileResponse();
        technicianProfile.setId("tech-1");
        technicianProfile.setUserId("tech-user-1");
        technicianProfile.setAvailable(true);
        technicianProfile.setCurrentWorkload(2);
        technicianProfile.setMaxWorkload(5);
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

        when(identityClient.getCustomer("customer-1")).thenReturn(customerSummary);
        when(itemRepository.existsById("service-1")).thenReturn(true);
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doNothing().when(notificationClient).sendNotification(any());

        ServiceRequestResponse response = serviceRequestService.create(request, "customer-1");

        assertNotNull(response);
        assertEquals("req-1", response.getId());
        verify(identityClient, times(1)).getCustomer("customer-1");
        verify(itemRepository, times(1)).existsById("service-1");
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenPriorityInvalid() {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setServiceId("service-1");
        request.setPriority("INVALID");
        request.setPreferredDate(Instant.now().plusSeconds(3600));
        request.setAddress("123 Main St");

        when(identityClient.getCustomer("customer-1")).thenReturn(customerSummary);
        when(itemRepository.existsById("service-1")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> serviceRequestService.create(request, "customer-1"));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenPreferredDateInPast() {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setServiceId("service-1");
        request.setPriority("HIGH");
        request.setPreferredDate(Instant.now().minusSeconds(3600));
        request.setAddress("123 Main St");

        when(identityClient.getCustomer("customer-1")).thenReturn(customerSummary);
        when(itemRepository.existsById("service-1")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> serviceRequestService.create(request, "customer-1"));
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

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(technicianClient.getTechnician("tech-1")).thenReturn(technicianProfile);
        doNothing().when(technicianClient).updateWorkload(anyString(), anyInt());
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doNothing().when(notificationClient).sendNotification(any());

        ServiceRequestResponse response = serviceRequestService.assign("req-1", assignRequest);

        assertNotNull(response);
//        verify(technicianClient, times(1)).getTechnician("tech-1");
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void assign_ShouldThrowBadRequest_WhenRequestCompleted() {
        AssignRequest assignRequest = new AssignRequest();
        assignRequest.setTechnicianId("tech-1");

        serviceRequest.setStatus(RequestStatus.COMPLETED);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.assign("req-1", assignRequest));
    }

    @Test
    void updateStatus_ShouldUpdateStatus() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("ASSIGNED");

        serviceRequest.setTechnicianId("tech-1");
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);

        ServiceRequestResponse response = serviceRequestService.updateStatus("req-1", request);

        assertNotNull(response);
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

//    @Test
//    void cancel_ShouldCancelRequest() {
//        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
//        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
//        doNothing().when(notificationClient).sendNotification(any());
//
//        ServiceRequestResponse response = serviceRequestService.cancel("req-1", "customer-1");
//
//        assertNotNull(response);
//        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
//    }

    @Test
    void cancel_ShouldThrowUnauthorized_WhenUserNotOwner() {
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(UnauthorizedException.class, () -> serviceRequestService.cancel("req-1", "different-customer"));
    }

    @Test
    void stats_ShouldReturnStats() {
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        when(requestRepository.findAll()).thenReturn(requests);

        ServiceRequestStatsResponse response = serviceRequestService.stats();

        assertNotNull(response);
        assertNotNull(response.getByStatus());
        verify(requestRepository, times(1)).findAll();
    }

    @Test
    void getByTechnicianUserId_ShouldReturnRequests() {
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findByTechnicianId("tech-1")).thenReturn(requests);

        List<ServiceRequestResponse> responses = serviceRequestService.getByTechnicianUserId("tech-user-1");

        assertNotNull(responses);
        verify(technicianClient, times(1)).getTechnicianByUserId("tech-user-1");
        verify(requestRepository, times(1)).findByTechnicianId("tech-1");
    }
}

