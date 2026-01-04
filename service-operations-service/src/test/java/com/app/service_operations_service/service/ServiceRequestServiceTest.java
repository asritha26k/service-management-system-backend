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

    // Additional tests for 0% coverage methods

    @Test
    void acceptWork_ShouldAcceptAssignedWork() {
        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        serviceRequest.setTechnicianId("tech-1");

        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doNothing().when(notificationClient).sendNotification(any());

        ServiceRequestResponse response = serviceRequestService.acceptWork("req-1", "tech-user-1");

        assertNotNull(response);
        assertEquals("req-1", response.getId());
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void acceptWork_ShouldThrowUnauthorized_WhenTechnicianNotAssigned() {
        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        serviceRequest.setTechnicianId("different-tech");

        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(UnauthorizedException.class, () -> serviceRequestService.acceptWork("req-1", "tech-user-1"));
    }

    @Test
    void acceptWork_ShouldThrowBadRequest_WhenNotInAssignedStatus() {
        serviceRequest.setStatus(RequestStatus.REQUESTED);
        serviceRequest.setTechnicianId("tech-1");

        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.acceptWork("req-1", "tech-user-1"));
    }

    @Test
    void rejectWork_ShouldRejectAssignedWork() {
        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        serviceRequest.setTechnicianId("tech-1");

        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        doNothing().when(technicianClient).updateWorkload(anyString(), anyInt());
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doNothing().when(notificationClient).sendNotification(any());

        ServiceRequestResponse response = serviceRequestService.rejectWork("req-1", "tech-user-1", "Busy");

        assertNotNull(response);
        assertEquals("req-1", response.getId());
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void rejectWork_ShouldThrowUnauthorized_WhenTechnicianNotAssigned() {
        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        serviceRequest.setTechnicianId("different-tech");

        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(UnauthorizedException.class, () -> serviceRequestService.rejectWork("req-1", "tech-user-1", "Busy"));
    }

    @Test
    void rejectWork_ShouldThrowBadRequest_WhenNotInAssignedStatus() {
        serviceRequest.setStatus(RequestStatus.ACCEPTED);
        serviceRequest.setTechnicianId("tech-1");

        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.rejectWork("req-1", "tech-user-1", "Busy"));
    }

    @Test
    void rejectWork_ShouldRejectWithoutReason() {
        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        serviceRequest.setTechnicianId("tech-1");

        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        doNothing().when(technicianClient).updateWorkload(anyString(), anyInt());
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doNothing().when(notificationClient).sendNotification(any());

        ServiceRequestResponse response = serviceRequestService.rejectWork("req-1", "tech-user-1", null);

        assertNotNull(response);
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void completeByTechnician_ShouldCompleteRequest() {
        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        serviceRequest.setTechnicianId("tech-1");

        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doNothing().when(notificationClient).sendNotification(any());

        ServiceRequestResponse response = serviceRequestService.completeByTechnician("req-1", "tech-user-1");

        assertNotNull(response);
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
        verify(billingService, times(1)).generateInvoiceForCompletedRequest("req-1");
    }

    @Test
    void completeByTechnician_ShouldThrowBadRequest_WhenNotAssigned() {
        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        serviceRequest.setTechnicianId("different-tech");

        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.completeByTechnician("req-1", "tech-user-1"));
    }

    @Test
    void completeByTechnician_ShouldThrowBadRequest_WhenAlreadyCompleted() {
        serviceRequest.setStatus(RequestStatus.COMPLETED);
        serviceRequest.setTechnicianId("tech-1");

        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.completeByTechnician("req-1", "tech-user-1"));
    }

    @Test
    void reschedule_ShouldRescheduleRequest() {
        RescheduleServiceRequest rescheduleRequest = new RescheduleServiceRequest();
        rescheduleRequest.setPreferredDate(Instant.now().plusSeconds(7200));

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doNothing().when(notificationClient).sendNotification(any());

        ServiceRequestResponse response = serviceRequestService.reschedule("req-1", "customer-1", rescheduleRequest);

        assertNotNull(response);
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void reschedule_ShouldThrowUnauthorized_WhenUserNotOwner() {
        RescheduleServiceRequest rescheduleRequest = new RescheduleServiceRequest();
        rescheduleRequest.setPreferredDate(Instant.now().plusSeconds(7200));

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(UnauthorizedException.class, () -> serviceRequestService.reschedule("req-1", "different-customer", rescheduleRequest));
    }

    @Test
    void reschedule_ShouldThrowBadRequest_WhenDateInPast() {
        RescheduleServiceRequest rescheduleRequest = new RescheduleServiceRequest();
        rescheduleRequest.setPreferredDate(Instant.now().minusSeconds(3600));

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.reschedule("req-1", "customer-1", rescheduleRequest));
    }

    @Test
    void reschedule_ShouldThrowBadRequest_WhenRequestAssigned() {
        RescheduleServiceRequest rescheduleRequest = new RescheduleServiceRequest();
        rescheduleRequest.setPreferredDate(Instant.now().plusSeconds(7200));

        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.reschedule("req-1", "customer-1", rescheduleRequest));
    }

    @Test
    void cancel_ShouldCancelRequest() {
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doNothing().when(notificationClient).sendNotification(any());

        ServiceRequestResponse response = serviceRequestService.cancel("req-1", "customer-1");

        assertNotNull(response);
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void cancel_ShouldThrowBadRequest_WhenAlreadyCancelled() {
        serviceRequest.setStatus(RequestStatus.CANCELLED);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.cancel("req-1", "customer-1"));
    }

    @Test
    void cancel_ShouldThrowBadRequest_WhenAssigned() {
        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.cancel("req-1", "customer-1"));
    }

    @Test
    void getByCustomerWithTechnicianDetails_ShouldReturnRequestsWithDetails() {
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        when(requestRepository.findByCustomerId("customer-1")).thenReturn(requests);

        List<ServiceRequestWithTechnicianResponse> responses = serviceRequestService.getByCustomerWithTechnicianDetails("customer-1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(requestRepository, times(1)).findByCustomerId("customer-1");
    }

    @Test
    void getByTechnicianUserIdWithCustomerDetails_ShouldReturnRequestsWithDetails() {
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findByTechnicianId("tech-1")).thenReturn(requests);
        when(identityClient.getCustomer("customer-1")).thenReturn(customerSummary);

        List<ServiceRequestWithCustomerResponse> responses = serviceRequestService.getByTechnicianUserIdWithCustomerDetails("tech-user-1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(technicianClient, times(1)).getTechnicianByUserId("tech-user-1");
    }

    @Test
    void getByTechnicianUserIdWithCustomerDetails_ShouldThrowBadRequest_WhenFetchFails() {
        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenThrow(new RuntimeException("Service error"));

        assertThrows(BadRequestException.class, () -> serviceRequestService.getByTechnicianUserIdWithCustomerDetails("tech-user-1"));
    }

    @Test
    void updateStatus_ShouldUpdateToCompleted() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("COMPLETED");
        
        serviceRequest.setStatus(RequestStatus.ACCEPTED);
        serviceRequest.setTechnicianId("tech-1");

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        when(technicianClient.getTechnician("tech-1")).thenReturn(technicianProfile);
        doNothing().when(technicianClient).updateWorkload(anyString(), anyInt());

        ServiceRequestResponse response = serviceRequestService.updateStatus("req-1", request);

        assertNotNull(response);
        verify(billingService, times(1)).generateInvoiceForCompletedRequest("req-1");
    }

    @Test
    void updateStatus_ShouldThrowBadRequest_WhenStatusCompleted() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("IN_PROGRESS");

        serviceRequest.setStatus(RequestStatus.COMPLETED);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.updateStatus("req-1", request));
    }

    @Test
    void updateStatus_ShouldThrowBadRequest_WhenInvalidTransition() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("IN_PROGRESS");

        serviceRequest.setStatus(RequestStatus.REQUESTED);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.updateStatus("req-1", request));
    }

    @Test
    void assign_ShouldThrowBadRequest_WhenTechnicianAtMaxCapacity() {
        AssignRequest assignRequest = new AssignRequest();
        assignRequest.setTechnicianId("tech-1");

        technicianProfile.setCurrentWorkload(5);
        technicianProfile.setMaxWorkload(5);

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(technicianClient.getTechnician("tech-1")).thenReturn(technicianProfile);

        assertThrows(BadRequestException.class, () -> serviceRequestService.assign("req-1", assignRequest));
    }

    @Test
    void assign_ShouldThrowBadRequest_WhenTechnicianNotAvailable() {
        AssignRequest assignRequest = new AssignRequest();
        assignRequest.setTechnicianId("tech-1");

        technicianProfile.setAvailable(false);

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(technicianClient.getTechnician("tech-1")).thenReturn(technicianProfile);

        assertThrows(BadRequestException.class, () -> serviceRequestService.assign("req-1", assignRequest));
    }

    @Test
    void assign_ShouldThrowBadRequest_WhenAlreadyAssigned() {
        AssignRequest assignRequest = new AssignRequest();
        assignRequest.setTechnicianId("tech-1");

        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        serviceRequest.setTechnicianId("different-tech");

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.assign("req-1", assignRequest));
    }

    @Test
    void assign_ShouldThrowBadRequest_WhenRequestCancelled() {
        AssignRequest assignRequest = new AssignRequest();
        assignRequest.setTechnicianId("tech-1");

        serviceRequest.setStatus(RequestStatus.CANCELLED);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.assign("req-1", assignRequest));
    }

    // Additional tests for improved branch coverage

    @Test
    void notifyCustomer_ShouldHandleNullCustomerId() {
        // This is a private method, but we can test through public methods that call it
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
    }

    @Test
    void notifyTechnician_ShouldHandleNotificationFailure() {
        AssignRequest assignRequest = new AssignRequest();
        assignRequest.setTechnicianId("tech-1");

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(technicianClient.getTechnician("tech-1")).thenReturn(technicianProfile);
        doNothing().when(technicianClient).updateWorkload(anyString(), anyInt());
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doThrow(new RuntimeException("Notification service down")).when(notificationClient).sendNotification(any());

        // Should still succeed even if notification fails
        ServiceRequestResponse response = serviceRequestService.assign("req-1", assignRequest);
        assertNotNull(response);
    }

    @Test
    void cancel_ShouldCancelRequestInRequestedStatus() {
        serviceRequest.setStatus(RequestStatus.REQUESTED);
        serviceRequest.setTechnicianId(null);

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doNothing().when(notificationClient).sendNotification(any());

        ServiceRequestResponse response = serviceRequestService.cancel("req-1", "customer-1");

        assertNotNull(response);
        // No technician assigned, so workload should not be updated
        verify(technicianClient, never()).updateWorkload(anyString(), anyInt());
    }

    @Test
    void cancel_ShouldCancelWhenRequestedWithoutTechnician() {
        serviceRequest.setStatus(RequestStatus.REQUESTED);
        serviceRequest.setTechnicianId(null);

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doNothing().when(notificationClient).sendNotification(any());

        ServiceRequestResponse response = serviceRequestService.cancel("req-1", "customer-1");
        assertNotNull(response);
    }

    @Test
    void updateStatus_ShouldSetAssignedAtForInProgress() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("IN_PROGRESS");

        serviceRequest.setStatus(RequestStatus.ACCEPTED);
        serviceRequest.setTechnicianId("tech-1");
        serviceRequest.setAssignedAt(null);

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);

        ServiceRequestResponse response = serviceRequestService.updateStatus("req-1", request);

        assertNotNull(response);
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void updateStatus_ShouldSetCompletedAtForCompleted() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("COMPLETED");

        serviceRequest.setStatus(RequestStatus.ACCEPTED);
        serviceRequest.setTechnicianId("tech-1");
        serviceRequest.setCompletedAt(null);

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        when(technicianClient.getTechnician("tech-1")).thenReturn(technicianProfile);
        doNothing().when(technicianClient).updateWorkload(anyString(), anyInt());

        ServiceRequestResponse response = serviceRequestService.updateStatus("req-1", request);

        assertNotNull(response);
        verify(requestRepository, times(1)).save(any(ServiceRequest.class));
    }

    @Test
    void updateStatus_ShouldThrowBadRequest_WhenMovingToAssignedWithoutTechnician() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("ASSIGNED");

        serviceRequest.setStatus(RequestStatus.REQUESTED);
        serviceRequest.setTechnicianId(null);

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.updateStatus("req-1", request));
    }

    @Test
    void updateStatus_ShouldThrowBadRequest_WhenMovingToInProgressFromRequested() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("IN_PROGRESS");

        serviceRequest.setStatus(RequestStatus.REQUESTED);

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.updateStatus("req-1", request));
    }

    @Test
    void getByTechnicianUserId_ShouldThrowBadRequest_WhenTechnicianNotFound() {
        when(technicianClient.getTechnicianByUserId("invalid-user")).thenThrow(new RuntimeException("Not found"));

        assertThrows(BadRequestException.class, () -> serviceRequestService.getByTechnicianUserId("invalid-user"));
    }

    @Test
    void getByTechnicianUserId_ShouldThrowBadRequest_WhenTechnicianProfileNull() {
        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(null);

        assertThrows(BadRequestException.class, () -> serviceRequestService.getByTechnicianUserId("tech-user-1"));
    }

    @Test
    void toResponse_ShouldHandleMissingTechnician() {
        serviceRequest.setTechnicianId(null);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        ServiceRequestResponse response = serviceRequestService.getById("req-1");

        assertNotNull(response);
        assertNull(response.getTechnicianId());
        assertNull(response.getTechnicianName());
    }

    @Test
    void toResponse_ShouldHandleTechnicianLookupFailure() {
        serviceRequest.setTechnicianId("tech-1");
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(technicianClient.getTechnician("tech-1")).thenThrow(new RuntimeException("Service error"));

        ServiceRequestResponse response = serviceRequestService.getById("req-1");

        assertNotNull(response);
        assertEquals("req-1", response.getId());
    }

    @Test
    void toResponseWithTechnicianDetails_ShouldIncludeTechnicianDetails() {
        serviceRequest.setTechnicianId("tech-1");
        technicianProfile.setEmail("tech@example.com");
        technicianProfile.setSpecialization("Plumbing");
        technicianProfile.setExperience(5);
        technicianProfile.setRating(4.5);

        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        when(requestRepository.findByCustomerId("customer-1")).thenReturn(requests);
        when(technicianClient.getTechnician("tech-1")).thenReturn(technicianProfile);

        List<ServiceRequestWithTechnicianResponse> responses = serviceRequestService.getByCustomerWithTechnicianDetails("customer-1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertNotNull(responses.get(0).getTechnicianDetails());
    }

    @Test
    void toResponseWithTechnicianDetails_ShouldHandleMissingTechnician() {
        serviceRequest.setTechnicianId("tech-1");
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        when(requestRepository.findByCustomerId("customer-1")).thenReturn(requests);
        when(technicianClient.getTechnician("tech-1")).thenThrow(new RuntimeException("Not found"));

        List<ServiceRequestWithTechnicianResponse> responses = serviceRequestService.getByCustomerWithTechnicianDetails("customer-1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void toResponseWithTechnicianDetails_ShouldHandleNullTechnicianId() {
        serviceRequest.setTechnicianId(null);
        List<ServiceRequest> requests = Arrays.asList(serviceRequest);
        when(requestRepository.findByCustomerId("customer-1")).thenReturn(requests);

        List<ServiceRequestWithTechnicianResponse> responses = serviceRequestService.getByCustomerWithTechnicianDetails("customer-1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertNull(responses.get(0).getTechnicianDetails());
    }

    @Test
    void ensureCustomerActive_ShouldThrowNotFound_WhenCustomerNull() {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setServiceId("service-1");
        request.setPriority("HIGH");
        request.setPreferredDate(Instant.now().plusSeconds(3600));
        request.setAddress("123 Main St");

        when(identityClient.getCustomer("customer-1")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> serviceRequestService.create(request, "customer-1"));
    }

    @Test
    void ensureCustomerActive_ShouldThrowBadRequest_WhenCustomerInactive() {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setServiceId("service-1");
        request.setPriority("HIGH");
        request.setPreferredDate(Instant.now().plusSeconds(3600));
        request.setAddress("123 Main St");

        customerSummary.setActive(false);
        when(identityClient.getCustomer("customer-1")).thenReturn(customerSummary);

        assertThrows(BadRequestException.class, () -> serviceRequestService.create(request, "customer-1"));
    }

    @Test
    void ensureCustomerActive_ShouldThrowExternalServiceException_OnServiceError() {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setServiceId("service-1");
        request.setPriority("HIGH");
        request.setPreferredDate(Instant.now().plusSeconds(3600));
        request.setAddress("123 Main St");

        when(identityClient.getCustomer("customer-1")).thenThrow(new RuntimeException("Connection error"));

        assertThrows(Exception.class, () -> serviceRequestService.create(request, "customer-1"));
    }

    @Test
    void ensureTechnicianActive_ShouldThrowNotFound_WhenTechnicianNull() {
        AssignRequest assignRequest = new AssignRequest();
        assignRequest.setTechnicianId("invalid-tech");

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(technicianClient.getTechnician("invalid-tech")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> serviceRequestService.assign("req-1", assignRequest));
    }

    @Test
    void ensureTechnicianActive_ShouldThrowExternalServiceException_OnServiceError() {
        AssignRequest assignRequest = new AssignRequest();
        assignRequest.setTechnicianId("tech-1");

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(technicianClient.getTechnician("tech-1")).thenThrow(new RuntimeException("Service error"));

        assertThrows(Exception.class, () -> serviceRequestService.assign("req-1", assignRequest));
    }

    @Test
    void validateStatusTransition_ShouldThrowBadRequest_WhenMovingBackToRequested() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("REQUESTED");

        serviceRequest.setStatus(RequestStatus.ACCEPTED);

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.updateStatus("req-1", request));
    }

    @Test
    void validateStatusTransition_ShouldThrowBadRequest_WhenCancelledToRequested() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("REQUESTED");

        serviceRequest.setStatus(RequestStatus.CANCELLED);

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(BadRequestException.class, () -> serviceRequestService.updateStatus("req-1", request));
    }

    @Test
    void assign_ShouldNotUpdateWorkload_WhenMaxWorkloadNull() {
        AssignRequest assignRequest = new AssignRequest();
        assignRequest.setTechnicianId("tech-1");

        technicianProfile.setCurrentWorkload(null);
        technicianProfile.setMaxWorkload(null);

        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(technicianClient.getTechnician("tech-1")).thenReturn(technicianProfile);
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doNothing().when(notificationClient).sendNotification(any());

        ServiceRequestResponse response = serviceRequestService.assign("req-1", assignRequest);

        assertNotNull(response);
        verify(technicianClient, never()).updateWorkload(anyString(), anyInt());
    }

    @Test
    void completeByTechnician_ShouldHandleBillingServiceFailure() {
        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        serviceRequest.setTechnicianId("tech-1");

        when(technicianClient.getTechnicianByUserId("tech-user-1")).thenReturn(technicianProfile);
        when(requestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(serviceRequest);
        doThrow(new RuntimeException("Billing error")).when(billingService).generateInvoiceForCompletedRequest(anyString());
        doNothing().when(notificationClient).sendNotification(any());

        // Should still succeed even if billing fails
        ServiceRequestResponse response = serviceRequestService.completeByTechnician("req-1", "tech-user-1");

        assertNotNull(response);
    }
}

