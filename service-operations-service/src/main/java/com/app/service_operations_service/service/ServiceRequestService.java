package com.app.service_operations_service.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.service_operations_service.client.NotificationClient;
import com.app.service_operations_service.client.TechnicianClient;
import com.app.service_operations_service.client.dto.NotificationRequest;
import com.app.service_operations_service.client.dto.NotificationType;
import com.app.service_operations_service.client.dto.TechnicianProfileResponse;
import com.app.service_operations_service.dto.PagedResponse;
import com.app.service_operations_service.exception.NotFoundException;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.repository.ServiceRequestRepository;
import com.app.service_operations_service.util.ValidationUtil;
import com.app.service_operations_service.dto.requests.AssignRequest;
import com.app.service_operations_service.dto.requests.CreateServiceRequest;
import com.app.service_operations_service.dto.requests.RescheduleServiceRequest;
import com.app.service_operations_service.dto.requests.ServiceRequestResponse;
import com.app.service_operations_service.dto.requests.ServiceRequestStatsResponse;
import com.app.service_operations_service.dto.requests.ServiceRequestWithCustomerResponse;
import com.app.service_operations_service.dto.requests.ServiceRequestWithTechnicianResponse;
import com.app.service_operations_service.dto.requests.UpdateStatusRequest;

@Service
@Transactional
public class ServiceRequestService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestService.class);

    private static final String YOUR_REQUEST_PREFIX = "Your request ";
    private static final String USER_ID = "userId";
    private static final String REQUEST_ID = "requestId";

    private final ServiceRequestRepository requestRepository;
    private final NotificationClient notificationClient;
    private final TechnicianClient technicianClient;
    private final BillingService billingService;

    public ServiceRequestService(
            ServiceRequestRepository requestRepository,
            NotificationClient notificationClient,
            TechnicianClient technicianClient,
            @Lazy BillingService billingService) {
        this.requestRepository = requestRepository;
        this.notificationClient = notificationClient;
        this.technicianClient = technicianClient;
        this.billingService = billingService;
    }

    public ServiceRequestResponse create(CreateServiceRequest request, String customerId) {
        ValidationUtil.validateNotNull(request, "CreateServiceRequest");
        ValidationUtil.validateNotBlank(customerId, USER_ID);

        ServiceRequest entity = new ServiceRequest();
        entity.setRequestNumber(generateRequestNumber());
        entity.setCustomerId(customerId);
        entity.setServiceId(request.getServiceId());
        entity.setPriority(request.getPriority());
        entity.setPreferredDate(request.getPreferredDate());
        entity.setAddress(request.getAddress());

        requestRepository.save(entity);

        notifyCustomer(
                entity.getCustomerId(),
                "Request Created",
                YOUR_REQUEST_PREFIX + entity.getRequestNumber() + " is created."
        );

        return toResponse(entity);
    }


    public PagedResponse<ServiceRequestResponse> getAll(Pageable pageable) {
        Page<ServiceRequest> page = requestRepository.findAll(pageable);

        List<ServiceRequestResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    public List<ServiceRequestResponse> getByStatus(String status) {
        RequestStatus requestStatus = RequestStatus.valueOf(status.toUpperCase());
        return requestRepository.findByStatus(requestStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    public ServiceRequestResponse getById(String id) {
        ServiceRequest request = fetch(id);
        return toResponse(request);
    }

    public List<ServiceRequestResponse> getByCustomer(String customerId) {
        ValidationUtil.validateNotBlank(customerId, "customerId");
        return requestRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServiceRequestWithTechnicianResponse> getByCustomerWithTechnicianDetails(String customerId) {
        ValidationUtil.validateNotBlank(customerId, "customerId");
        return requestRepository.findByCustomerId(customerId).stream()
                .map(this::toResponseWithTechnicianDetails)
                .toList();
    }

    public List<ServiceRequestResponse> getByTechnicianUserId(String userId) {
        ValidationUtil.validateNotBlank(userId, USER_ID);
        log.debug("Fetching technician profile for userId: {}", userId);
        TechnicianProfileResponse technician = technicianClient.getTechnicianByUserId(userId);
        if (technician == null) {
            log.warn("No technician profile found for userId: {}", userId);
            return List.of();
        }
        log.debug("Found technician profile with id: {} for userId: {}", technician.getId(), userId);
        return requestRepository.findByTechnicianId(technician.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServiceRequestWithCustomerResponse> getByTechnicianUserIdWithCustomerDetails(String userId) {
        ValidationUtil.validateNotBlank(userId, USER_ID);
        TechnicianProfileResponse technician = technicianClient.getTechnicianByUserId(userId);
        if (technician == null) {
            return List.of();
        }
        return requestRepository.findByTechnicianId(technician.getId()).stream()
                .map(this::toResponseWithCustomerDetails)
                .toList();
    }

    public ServiceRequestResponse cancel(String id, String userId) {
        ValidationUtil.validateNotBlank(id, REQUEST_ID);
        ValidationUtil.validateNotBlank(userId, USER_ID);

        ServiceRequest request = fetch(id);
        request.setStatus(RequestStatus.CANCELLED);

        ServiceRequest saved = requestRepository.save(request);

        notifyCustomer(
                saved.getCustomerId(),
                "Request Cancelled",
                YOUR_REQUEST_PREFIX + saved.getRequestNumber() + " has been cancelled.");

        return toResponse(saved);
    }

    public ServiceRequestResponse reschedule(
            String id,
            String userId,
            RescheduleServiceRequest payload) {

        ValidationUtil.validateNotBlank(id, REQUEST_ID);
        ValidationUtil.validateNotBlank(userId, USER_ID);

        ServiceRequest request = fetch(id);
        request.setPreferredDate(payload.getPreferredDate());

        ServiceRequest saved = requestRepository.save(request);

        notifyCustomer(
                saved.getCustomerId(),
                "Request Rescheduled",
                YOUR_REQUEST_PREFIX + saved.getRequestNumber() + " has been rescheduled.");

        return toResponse(saved);
    }

    public void assign(String id, AssignRequest request) {
        ValidationUtil.validateNotBlank(id, REQUEST_ID);
        ValidationUtil.validateNotNull(request, "AssignRequest");

        ServiceRequest serviceRequest = fetch(id);
        serviceRequest.setTechnicianId(request.getTechnicianId());
        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        serviceRequest.setAssignedAt(Instant.now());
        ServiceRequest saved = requestRepository.save(serviceRequest);

        // Workload will be increased when technician accepts the request

        notifyTechnician(
                serviceRequest.getTechnicianId(),
                "Service Request Assigned",
                YOUR_REQUEST_PREFIX + serviceRequest.getRequestNumber()
                        + " has been assigned to you. Please review and accept or decline.");

        log.info("Technician assigned to request");
    }

    public void updateStatus(String id, UpdateStatusRequest request) {
        ValidationUtil.validateNotBlank(id, REQUEST_ID);
        ValidationUtil.validateNotNull(request, "UpdateStatusRequest");

        // If changing status to COMPLETED, delegate to the complete method
        // which handles workload decrease, invoice generation, and notifications
        if ("COMPLETED".equalsIgnoreCase(request.getStatus())) {
            ServiceRequest serviceRequest = fetch(id);
            // Use the technician ID as userId for completion
            if (serviceRequest.getTechnicianId() != null) {
                completeByTechnician(id, serviceRequest.getTechnicianId());
            } else {
                // Fallback: just update status
                serviceRequest.setStatus(RequestStatus.COMPLETED);
                requestRepository.save(serviceRequest);
            }
            return;
        }

        ServiceRequest serviceRequest = fetch(id);
        serviceRequest.setStatus(RequestStatus.valueOf(request.getStatus().toUpperCase()));
        requestRepository.save(serviceRequest);

        log.info("Status of request updated");
    }

    public void acceptWork(String id, String userId) {
        ValidationUtil.validateNotBlank(id, REQUEST_ID);
        ValidationUtil.validateNotBlank(userId, USER_ID);

        ServiceRequest request = fetch(id);
        request.setStatus(RequestStatus.ACCEPTED);
        request.setAcceptedAt(Instant.now());
        ServiceRequest saved = requestRepository.save(request);

        // Increase technician workload when accepting
        if (saved.getTechnicianId() != null) {
            TechnicianProfileResponse technician = technicianClient.getTechnician(saved.getTechnicianId());
            if (technician != null) {
                int newWorkload = (technician.getCurrentWorkload() != null ? technician.getCurrentWorkload() : 0) + 1;
                technicianClient.updateWorkload(saved.getTechnicianId(), newWorkload);
            }
        }

    }

    public void rejectWork(String id, String userId, String reason) {
        ValidationUtil.validateNotBlank(id, REQUEST_ID);
        ValidationUtil.validateNotBlank(userId, USER_ID);
        ValidationUtil.validateNotBlank(reason, "reason");

        ServiceRequest request = fetch(id);
        request.setStatus(RequestStatus.CANCELLED);
        ServiceRequest saved = requestRepository.save(request);

    }

    public ServiceRequestResponse completeByTechnician(
            String requestId,
            String userId) {

        ValidationUtil.validateNotBlank(requestId, REQUEST_ID);
        ValidationUtil.validateNotBlank(userId, USER_ID);

        ServiceRequest request = fetch(requestId);
        request.setStatus(RequestStatus.COMPLETED);
        request.setCompletedAt(Instant.now());

        ServiceRequest saved = requestRepository.save(request);

        // Decrease technician workload when completing
        if (request.getTechnicianId() != null) {
            TechnicianProfileResponse technician = technicianClient.getTechnician(request.getTechnicianId());
            if (technician != null) {
                int newWorkload = Math.max(0,
                        (technician.getCurrentWorkload() != null ? technician.getCurrentWorkload() : 1) - 1);
                technicianClient.updateWorkload(request.getTechnicianId(), newWorkload);
            }
        }

        // Auto-generate invoice for completed request
        try {
            billingService.generateInvoiceForCompletedRequest(requestId);
        } catch (Exception e) {
            log.error("Failed to generate invoice for request {}: {}", requestId, e.getMessage());
            // Don't fail the completion if invoice generation fails
        }

        notifyCustomer(
                saved.getCustomerId(),
                "Service Completed",
                YOUR_REQUEST_PREFIX + saved.getRequestNumber()
                        + " has been completed. Invoice has been generated.");

        return toResponse(saved);
    }

    public ServiceRequestStatsResponse stats() {
        long totalRequests = requestRepository.count();
        long completedRequests = requestRepository.countByStatus(RequestStatus.COMPLETED);
        long requestedRequests = requestRepository.countByStatus(RequestStatus.REQUESTED);
        long inProgressRequests = requestRepository.countByStatus(RequestStatus.IN_PROGRESS);

        ServiceRequestStatsResponse response = new ServiceRequestStatsResponse();
        response.setTotal(totalRequests);
        Map<RequestStatus, Long> byStatus = new java.util.EnumMap<>(RequestStatus.class);
        byStatus.put(RequestStatus.COMPLETED, completedRequests);
        byStatus.put(RequestStatus.REQUESTED, requestedRequests);
        byStatus.put(RequestStatus.IN_PROGRESS, inProgressRequests);
        response.setByStatus(byStatus);
        return response;
    }

    private ServiceRequest fetch(String id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service request not found: " + id));
    }

    private ServiceRequestResponse toResponse(ServiceRequest request) {
        ServiceRequestResponse response = new ServiceRequestResponse();
        response.setId(request.getId());
        response.setRequestNumber(request.getRequestNumber());
        response.setCustomerId(request.getCustomerId());
        response.setServiceId(request.getServiceId());
        response.setPriority(request.getPriority());
        response.setStatus(request.getStatus());
        response.setPreferredDate(request.getPreferredDate());
        response.setAddress(request.getAddress());
        response.setTechnicianId(request.getTechnicianId());
        response.setAssignedAt(request.getAssignedAt());
        response.setAcceptedAt(request.getAcceptedAt());
        response.setCompletedAt(request.getCompletedAt());
        response.setCreatedAt(request.getCreatedAt());

        // Fetch technician details if assigned
        if (request.getTechnicianId() != null) {
            TechnicianProfileResponse technician = technicianClient.getTechnician(request.getTechnicianId());
            if (technician != null) {
                response.setTechnicianName(technician.getName());
                response.setTechnicianPhone(technician.getPhone());
            }
        }

        return response;
    }

    private ServiceRequestWithTechnicianResponse toResponseWithTechnicianDetails(ServiceRequest request) {
        ServiceRequestWithTechnicianResponse response = new ServiceRequestWithTechnicianResponse();
        response.setId(request.getId());
        response.setRequestNumber(request.getRequestNumber());
        response.setCustomerId(request.getCustomerId());
        response.setServiceId(request.getServiceId());
        response.setPriority(request.getPriority());
        response.setStatus(request.getStatus());
        response.setPreferredDate(request.getPreferredDate());
        response.setAddress(request.getAddress());
        response.setCreatedAt(request.getCreatedAt());
        response.setTechnicianId(request.getTechnicianId());
        return response;
    }

    private ServiceRequestWithCustomerResponse toResponseWithCustomerDetails(ServiceRequest request) {
        ServiceRequestWithCustomerResponse response = new ServiceRequestWithCustomerResponse();
        response.setId(request.getId());
        response.setRequestNumber(request.getRequestNumber());
        response.setCustomerId(request.getCustomerId());
        response.setServiceId(request.getServiceId());
        response.setPriority(request.getPriority());
        response.setStatus(request.getStatus());
        response.setPreferredDate(request.getPreferredDate());
        response.setAddress(request.getAddress());
        response.setCreatedAt(request.getCreatedAt());
        return response;
    }

    private void notifyCustomer(String customerId, String subject, String message) {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(customerId);
        request.setType(NotificationType.IN_APP);
        request.setSubject(subject);
        request.setMessage(message);
        notificationClient.sendNotification(request);
    }

    private void notifyTechnician(String technicianId, String subject, String message) {
        // technicianId is a profile ID, we need to get the actual userId
        TechnicianProfileResponse technician = technicianClient.getTechnician(technicianId);
        if (technician == null) {
            log.warn("Technician profile not found for ID: {}", technicianId);
            return;
        }

        NotificationRequest request = new NotificationRequest();
        request.setUserId(technician.getUserId()); // Use actual user ID
        request.setType(NotificationType.IN_APP);
        request.setSubject(subject);
        request.setMessage(message);
        notificationClient.sendNotification(request);
    }

    private String generateRequestNumber() {
        return "REQ-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
