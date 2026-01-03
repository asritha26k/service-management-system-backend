package com.app.service_operations_service.service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.service_operations_service.client.IdentityClient;
import com.app.service_operations_service.client.NotificationClient;
import com.app.service_operations_service.client.TechnicianClient;
import com.app.service_operations_service.client.dto.CustomerSummary;
import com.app.service_operations_service.client.dto.NotificationRequest;
import com.app.service_operations_service.client.dto.NotificationType;
import com.app.service_operations_service.client.dto.TechnicianProfileResponse;
import com.app.service_operations_service.dto.requests.AssignRequest;
import com.app.service_operations_service.dto.requests.CreateServiceRequest;
import com.app.service_operations_service.dto.requests.ServiceRequestResponse;
import com.app.service_operations_service.dto.requests.ServiceRequestStatsResponse;
import com.app.service_operations_service.dto.requests.ServiceRequestWithCustomerResponse;
import com.app.service_operations_service.dto.requests.ServiceRequestWithTechnicianResponse;
import com.app.service_operations_service.dto.requests.UpdateStatusRequest;
import com.app.service_operations_service.dto.requests.RescheduleServiceRequest;
import com.app.service_operations_service.exception.BadRequestException;
import com.app.service_operations_service.exception.ExternalServiceException;
import com.app.service_operations_service.exception.NotFoundException;
import com.app.service_operations_service.exception.UnauthorizedException;
import com.app.service_operations_service.model.ServiceRequest;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.repository.ServiceItemRepository;
import com.app.service_operations_service.repository.ServiceRequestRepository;
import com.app.service_operations_service.util.ValidationUtil;

@Service
@Transactional
public class ServiceRequestService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestService.class);

    private final ServiceRequestRepository requestRepository;
    private final ServiceItemRepository itemRepository;
    private final IdentityClient identityClient;
    private final TechnicianClient technicianClient;
    private final NotificationClient notificationClient;
    private final BillingService billingService;

    public ServiceRequestService(
            ServiceRequestRepository requestRepository,
            ServiceItemRepository itemRepository,
            IdentityClient identityClient,
            TechnicianClient technicianClient,
            NotificationClient notificationClient,
            BillingService billingService) {
        this.requestRepository = requestRepository;
        this.itemRepository = itemRepository;
        this.identityClient = identityClient;
        this.technicianClient = technicianClient;
        this.notificationClient = notificationClient;
        this.billingService = billingService;
    }

    public List<ServiceRequestResponse> getAll() {
        return requestRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServiceRequestResponse> getByStatus(String status) {
        RequestStatus parsed = parseStatus(status);
        return requestRepository.findByStatus(parsed)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ServiceRequestResponse create(CreateServiceRequest request, String customerId) {
        // Validate input
        ValidationUtil.validateNotNull(request, "CreateServiceRequest");
        ValidationUtil.validateNotBlank(customerId, "customerId");
        ValidationUtil.validateNotBlank(request.getServiceId(), "serviceId");
        ValidationUtil.validateNotBlank(request.getAddress(), "address");
        ValidationUtil.validateNotBlank(request.getPriority(), "priority");
        ValidationUtil.validateLength(request.getAddress(), 500, "address");

        ensureCustomerActive(customerId);
        ensureServiceExists(request.getServiceId());

        // Business Rule: Validate priority is valid
        String priority = request.getPriority().toUpperCase();
        if (!priority.equals("LOW") && !priority.equals("MEDIUM") &&
                !priority.equals("HIGH") && !priority.equals("URGENT")) {
            throw new BadRequestException("Priority must be one of: LOW, MEDIUM, HIGH, URGENT");
        }

        // Business Rule: Validate preferred date is in the future
        if (request.getPreferredDate() != null && request.getPreferredDate().isBefore(Instant.now())) {
            throw new BadRequestException("Preferred date must be in the future");
        }

        ServiceRequest entity = new ServiceRequest();
        entity.setRequestNumber(generateRequestNumber());
        entity.setCustomerId(customerId);
        entity.setServiceId(request.getServiceId());
        entity.setPriority(priority);
        entity.setPreferredDate(request.getPreferredDate());
        entity.setAddress(request.getAddress());

        ServiceRequest saved = requestRepository.save(entity);
        log.info("Service request created: {} for customer: {}", saved.getId(), customerId);

        notifyCustomer(
                saved.getCustomerId(),
                "Request Created",
                "Your request " + saved.getRequestNumber() + " is created.");

        return toResponse(saved);
    }

    public ServiceRequestResponse getById(String id) {
        return toResponse(fetch(id));
    }

    public List<ServiceRequestResponse> getByCustomer(String customerId) {
        return requestRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServiceRequestWithTechnicianResponse> getByCustomerWithTechnicianDetails(String customerId) {
        return requestRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponseWithTechnicianDetails)
                .toList();
    }

    public List<ServiceRequestResponse> getByTechnician(String technicianId) {
        return requestRepository.findByTechnicianId(technicianId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServiceRequestResponse> getByTechnicianUserId(String userId) {
        try {
            TechnicianProfileResponse technician = technicianClient.getTechnicianByUserId(userId);

            if (technician == null) {
                throw new NotFoundException("Technician profile not found for user: " + userId);
            }

            return getByTechnician(technician.getId());
        } catch (Exception ex) {
            throw new BadRequestException("Unable to fetch technician profile: " + ex.getMessage());
        }
    }

    public List<ServiceRequestWithCustomerResponse> getByTechnicianUserIdWithCustomerDetails(String userId) {
        try {
            TechnicianProfileResponse technician = technicianClient.getTechnicianByUserId(userId);

            if (technician == null) {
                throw new NotFoundException("Technician profile not found for user: " + userId);
            }

            return requestRepository.findByTechnicianId(technician.getId())
                    .stream()
                    .map(this::toResponseWithCustomerDetails)
                    .toList();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to fetch service requests: " + ex.getMessage());
        }
    }

    public ServiceRequestResponse assign(String id, AssignRequest assignRequest) {
        // Validate input
        ValidationUtil.validateNotBlank(id, "serviceRequestId");
        ValidationUtil.validateNotNull(assignRequest, "AssignRequest");
        ValidationUtil.validateNotBlank(assignRequest.getTechnicianId(), "technicianId");

        ServiceRequest request = fetch(id);

        // Business Rule: Cannot assign already completed or cancelled requests
        if (request.getStatus() == RequestStatus.COMPLETED) {
            log.warn("Attempt to assign completed request: {}", id);
            throw new BadRequestException("Cannot assign a completed service request");
        }

        if (request.getStatus() == RequestStatus.CANCELLED) {
            log.warn("Attempt to assign cancelled request: {}", id);
            throw new BadRequestException("Cannot assign a cancelled service request");
        }

        // Business Rule: Cannot reassign if already assigned
        if (request.getTechnicianId() != null && request.getStatus() == RequestStatus.ASSIGNED) {
            log.warn("Attempt to reassign already assigned request: {}", id);
            throw new BadRequestException("Service request is already assigned to a technician. Unassign first.");
        }

        ensureTechnicianActive(assignRequest.getTechnicianId());

        // Business Rule: Technician must be available
        TechnicianProfileResponse technician = technicianClient.getTechnician(assignRequest.getTechnicianId());
        if (technician == null) {
            log.error("Technician not found: {}", assignRequest.getTechnicianId());
            throw new NotFoundException("Technician not found");
        }

        if (!Boolean.TRUE.equals(technician.getAvailable())) {
            log.warn("Technician not available: {}", assignRequest.getTechnicianId());
            throw new BadRequestException("Technician is not available for new assignments");
        }

        // Business Rule: Technician must have capacity (workload < maxWorkload)
        if (technician.getCurrentWorkload() != null && technician.getMaxWorkload() != null) {
            Integer newWorkload = technician.getCurrentWorkload() + 1;
            if (newWorkload > technician.getMaxWorkload()) {
                log.warn("Technician {} workload capacity exceeded: {}/{}",
                        assignRequest.getTechnicianId(), newWorkload, technician.getMaxWorkload());
                throw new BadRequestException("Technician has reached maximum workload capacity");
            }
            technicianClient.updateWorkload(assignRequest.getTechnicianId(), newWorkload);
        }

        request.setTechnicianId(assignRequest.getTechnicianId());
        request.setStatus(RequestStatus.ASSIGNED);
        request.setAssignedAt(Instant.now());

        ServiceRequest saved = requestRepository.save(request);
        log.info("Service request assigned: {} to technician: {}", saved.getId(), assignRequest.getTechnicianId());

        notifyTechnician(
                assignRequest.getTechnicianId(),
                "New Service Assigned",
                "You have been assigned request " + saved.getRequestNumber());

        notifyCustomer(
                saved.getCustomerId(),
                "Technician Assigned",
                "A technician has been assigned to your request " + saved.getRequestNumber());

        return toResponse(saved);
    }

    public ServiceRequestResponse updateStatus(String id, UpdateStatusRequest updateStatusRequest) {
        ServiceRequest request = fetch(id);
        RequestStatus currentStatus = request.getStatus();
        RequestStatus newStatus = parseStatus(updateStatusRequest.getStatus());

        // Business Rule: Cannot change status if already completed
        if (currentStatus == RequestStatus.COMPLETED) {
            throw new BadRequestException("Cannot change status of a completed service request");
        }

        // Business Rule: Cannot change status if cancelled
        if (currentStatus == RequestStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status of a cancelled service request");
        }

        // Business Rule: Validate state transitions
        validateStatusTransition(currentStatus, newStatus, request);

        request.setStatus(newStatus);

        if (newStatus == RequestStatus.IN_PROGRESS && request.getAssignedAt() == null) {
            request.setAssignedAt(Instant.now());
        }

        if (newStatus == RequestStatus.COMPLETED) {
            request.setCompletedAt(Instant.now());
        }

        return toResponse(requestRepository.save(request));
    }

    private void validateStatusTransition(RequestStatus from, RequestStatus to, ServiceRequest request) {
        // Business Rule: Cannot move to ASSIGNED without a technician
        if (to == RequestStatus.ASSIGNED && request.getTechnicianId() == null) {
            throw new BadRequestException("Cannot set status to ASSIGNED without assigning a technician");
        }

        // Business Rule: Cannot move to IN_PROGRESS without ACCEPTED (must accept
        // first)
        if (to == RequestStatus.IN_PROGRESS && from != RequestStatus.ACCEPTED) {
            throw new BadRequestException(
                    "Cannot move to IN_PROGRESS without accepting first. Current status: " + from);
        }

        // Business Rule: Cannot move back to REQUESTED from later states except when
        // rejected
        if (to == RequestStatus.REQUESTED && from != RequestStatus.REQUESTED && from != RequestStatus.ASSIGNED) {
            throw new BadRequestException("Cannot move back to REQUESTED status from " + from);
        }
    }

    public ServiceRequestResponse cancel(String id, String userId) {
        // Validate input
        ValidationUtil.validateNotBlank(id, "requestId");
        ValidationUtil.validateNotBlank(userId, "userId");

        ServiceRequest request = fetch(id);

        // Business Rule: Only the customer who created the request can cancel it
        if (!request.getCustomerId().equals(userId)) {
            log.warn("Unauthorized cancellation attempt: User {} tried to cancel request of customer {}", userId,
                    request.getCustomerId());
            throw new UnauthorizedException("You can only cancel your own service requests");
        }

        // Business Rule: Cannot cancel
        // assigned/accepted/in-progress/completed/cancelled requests
        if (request.getStatus() == RequestStatus.ASSIGNED ||
                request.getStatus() == RequestStatus.ACCEPTED ||
                request.getStatus() == RequestStatus.IN_PROGRESS ||
                request.getStatus() == RequestStatus.COMPLETED) {
            log.warn("Attempt to cancel non-requested request (status {}): {}", request.getStatus(), id);
            throw new BadRequestException("Cannot cancel a request after it has been assigned or processed");
        }

        if (request.getStatus() == RequestStatus.CANCELLED) {
            log.warn("Attempt to cancel already cancelled request: {}", id);
            throw new BadRequestException("Service request is already cancelled");
        }

        // Business Rule: If technician is assigned, decrement their workload
        if (request.getTechnicianId() != null &&
                (request.getStatus() == RequestStatus.ASSIGNED || request.getStatus() == RequestStatus.ACCEPTED)) {
            try {
                TechnicianProfileResponse technician = technicianClient.getTechnician(request.getTechnicianId());
                if (technician != null && technician.getCurrentWorkload() != null
                        && technician.getCurrentWorkload() > 0) {
                    technicianClient.updateWorkload(request.getTechnicianId(), technician.getCurrentWorkload() - 1);
                }
            } catch (Exception ex) {
                log.warn("Failed to update technician workload during cancellation", ex);
            }
        }

        request.setStatus(RequestStatus.CANCELLED);
        ServiceRequest saved = requestRepository.save(request);
        log.info("Service request cancelled by customer: {} for request: {}", userId, id);

        notifyCustomer(
                saved.getCustomerId(),
                "Request Cancelled",
                "Your request " + saved.getRequestNumber() + " has been cancelled.");

        return toResponse(saved);
    }

    public ServiceRequestResponse reschedule(String id, String userId, RescheduleServiceRequest payload) {
        ValidationUtil.validateNotBlank(id, "requestId");
        ValidationUtil.validateNotBlank(userId, "userId");

        ServiceRequest request = fetch(id);

        // Only the customer who created the request can reschedule
        if (!request.getCustomerId().equals(userId)) {
            log.warn("Unauthorized reschedule attempt: User {} tried to reschedule request of customer {}", userId,
                    request.getCustomerId());
            throw new UnauthorizedException("You can only reschedule your own service requests");
        }

        // Cannot reschedule after assignment or when already processed
        if (request.getStatus() == RequestStatus.ASSIGNED ||
                request.getStatus() == RequestStatus.ACCEPTED ||
                request.getStatus() == RequestStatus.IN_PROGRESS ||
                request.getStatus() == RequestStatus.COMPLETED ||
                request.getStatus() == RequestStatus.CANCELLED) {
            log.warn("Attempt to reschedule non-requested request (status {}): {}", request.getStatus(), id);
            throw new BadRequestException("Cannot reschedule a request after it has been assigned or processed");
        }

        // Reuse preferred date validation: must be in the future
        if (payload.getPreferredDate() != null && payload.getPreferredDate().isBefore(Instant.now())) {
            throw new BadRequestException("Preferred date must be in the future");
        }

        request.setPreferredDate(payload.getPreferredDate());
        ServiceRequest saved = requestRepository.save(request);

        log.info("Service request rescheduled by customer: {} for request: {}", userId, id);

        notifyCustomer(
                saved.getCustomerId(),
                "Request Rescheduled",
                "Your request " + saved.getRequestNumber() + " has been rescheduled.");

        return toResponse(saved);
    }

    public ServiceRequestResponse completeByTechnician(String requestId, String userId) {
        TechnicianProfileResponse response = technicianClient.getTechnicianByUserId(userId);

        String technicianId = response.getId();
        ServiceRequest request = fetch(requestId);

        if (request.getTechnicianId() == null ||
                !request.getTechnicianId().equals(technicianId)) {
            throw new BadRequestException("You are not assigned to this service request");
        }

        if (request.getStatus() == RequestStatus.COMPLETED ||
                request.getStatus() == RequestStatus.CANCELLED) {
            throw new BadRequestException("Service request is already " +
                    request.getStatus().name().toLowerCase());
        }

        request.setStatus(RequestStatus.COMPLETED);
        request.setCompletedAt(Instant.now());

        ServiceRequest saved = requestRepository.save(request);

        // Decrement technician's workload after completion
        TechnicianProfileResponse technician = technicianClient.getTechnician(technicianId);
        if (technician != null && technician.getCurrentWorkload() != null && technician.getCurrentWorkload() > 0) {
            technicianClient.updateWorkload(technicianId, technician.getCurrentWorkload() - 1);
        }

        try {
            billingService.generateInvoiceForCompletedRequest(saved.getId());
        } catch (Exception ex) {
            log.error("Invoice generation failed for request {}", saved.getId(), ex);
        }

        notifyCustomer(
                saved.getCustomerId(),
                "Service Completed",
                "Your service request " + saved.getRequestNumber() +
                        " has been completed. Invoice has been generated.");

        return toResponse(saved);
    }

    public ServiceRequestStatsResponse stats() {
        List<ServiceRequest> all = requestRepository.findAll();

        Map<RequestStatus, Long> map = new EnumMap<>(RequestStatus.class);
        for (RequestStatus status : RequestStatus.values()) {
            map.put(status, 0L);
        }

        for (ServiceRequest r : all) {
            map.put(r.getStatus(), map.get(r.getStatus()) + 1);
        }

        ServiceRequestStatsResponse response = new ServiceRequestStatsResponse();
        response.setByStatus(map);
        response.setTotal(all.size());
        return response;
    }

    private ServiceRequest fetch(String id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service request not found: " + id));
    }

    private void ensureCustomerActive(String customerId) {
        try {
            CustomerSummary summary = identityClient.getCustomer(customerId);
            if (summary == null) {
                log.error("Customer not found: {}", customerId);
                throw new NotFoundException("Customer not found: " + customerId);
            }
            if (!summary.isActive()) {
                log.warn("Customer is inactive: {}", customerId);
                throw new BadRequestException("Customer is not active");
            }
        } catch (BadRequestException | NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to validate customer: {}", customerId, ex);
            throw new ExternalServiceException("Unable to validate customer: " + ex.getMessage(), ex);
        }
    }

    private void ensureServiceExists(String serviceId) {
        if (serviceId == null || serviceId.trim().isEmpty()) {
            throw new BadRequestException("Service ID cannot be empty");
        }
        if (!itemRepository.existsById(serviceId)) {
            log.warn("Service not found: {}", serviceId);
            throw new NotFoundException("Service item not found: " + serviceId);
        }
    }

    private void ensureTechnicianActive(String technicianId) {
        try {
            TechnicianProfileResponse technician = technicianClient.getTechnician(technicianId);

            if (technician == null) {
                log.error("Technician not found: {}", technicianId);
                throw new NotFoundException("Technician not found: " + technicianId);
            }
        } catch (NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to validate technician: {}", technicianId, ex);
            throw new ExternalServiceException("Unable to validate technician: " + ex.getMessage(), ex);
        }
    }

    private void notifyCustomer(String customerId, String subject, String message) {
        try {
            ValidationUtil.validateNotBlank(customerId, "customerId");
            ValidationUtil.validateNotBlank(subject, "subject");
            ValidationUtil.validateNotBlank(message, "message");

            log.info("=== NOTIFYING CUSTOMER ===");
            log.info("customerId: {}", customerId);

            // Get the customer profile which has the user ID mapping
            CustomerSummary customer = identityClient.getCustomer(customerId);
            if (customer == null) {
                log.warn("Customer not found for notification: {}", customerId);
                return;
            }

            // IMPORTANT: Use the customerId as userId since when JWT is created,
            // the user.userId() will be the same as customerId
            // This ensures notification query and save use the same userId
            String userId = customerId;
            log.info("Sending notification with userId: {} for customer: {}", userId, customerId);

            NotificationRequest request = new NotificationRequest();
            request.setUserId(userId);
            request.setType(NotificationType.IN_APP);
            request.setSubject(subject);
            request.setMessage(message);
            notificationClient.sendNotification(request);
            log.info("Notification sent successfully. userId: {}, subject: {}", userId, subject);
        } catch (Exception e) {
            log.error("Failed to notify customer {}: {}", customerId, e.getMessage(), e);
        }
    }

    private void notifyTechnician(String technicianId, String subject, String message) {
        try {
            ValidationUtil.validateNotBlank(technicianId, "technicianId");
            ValidationUtil.validateNotBlank(subject, "subject");
            ValidationUtil.validateNotBlank(message, "message");

            // Get the technician profile which has the user ID mapping
            TechnicianProfileResponse technician = technicianClient.getTechnician(technicianId);
            if (technician == null) {
                log.warn("Technician not found for notification: {}", technicianId);
                return;
            }

            // Use the user ID from technician profile
            String userId = technician.getUserId();
            if (userId == null) {
                log.warn("Technician {} has no mapped user ID", technicianId);
                return;
            }

            NotificationRequest request = new NotificationRequest();
            request.setUserId(userId);
            request.setType(NotificationType.IN_APP);
            request.setSubject(subject);
            request.setMessage(message);
            notificationClient.sendNotification(request);
            log.info("Notification sent to technician: {} (user ID: {})", technicianId, userId);
        } catch (Exception e) {
            log.error("Failed to notify technician {}: {}", technicianId, e.getMessage(), e);
        }
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
        response.setTechnicianId(request.getTechnicianId());
        response.setAssignedAt(request.getAssignedAt());
        response.setAcceptedAt(request.getAcceptedAt());
        response.setCompletedAt(request.getCompletedAt());
        response.setCreatedAt(request.getCreatedAt());

        if (request.getTechnicianId() != null) {
            try {
                TechnicianProfileResponse tech = technicianClient.getTechnician(request.getTechnicianId());

                if (tech != null) {
                    ServiceRequestWithTechnicianResponse.TechnicianDetails d = new ServiceRequestWithTechnicianResponse.TechnicianDetails();
                    d.setId(tech.getId());
                    d.setEmail(tech.getEmail());
                    d.setPhone(tech.getPhone());
                    d.setSpecialization(tech.getSpecialization());
                    d.setExperience(tech.getExperience());
                    d.setRating(tech.getRating());
                    response.setTechnicianDetails(d);
                }
            } catch (Exception ex) {
                log.error("Failed to fetch technician details", ex);
            }
        }

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
        response.setTechnicianId(request.getTechnicianId());
        response.setAssignedAt(request.getAssignedAt());
        response.setAcceptedAt(request.getAcceptedAt());
        response.setCompletedAt(request.getCompletedAt());
        response.setCreatedAt(request.getCreatedAt());

        if (request.getCustomerId() != null) {
            try {
                CustomerSummary customer = identityClient.getCustomer(request.getCustomerId());

                if (customer != null) {
                    ServiceRequestWithCustomerResponse.CustomerDetails details = new ServiceRequestWithCustomerResponse.CustomerDetails();
                    details.setId(customer.getId());
                    details.setName(customer.getName());
                    details.setEmail(customer.getEmail());
                    details.setPhone(customer.getPhone());
                    details.setAddress(customer.getAddress());
                    response.setCustomerDetails(details);
                }
            } catch (Exception ex) {
                log.error("Failed to fetch customer details for request {}", request.getId(), ex);
            }
        }

        return response;
    }

    private RequestStatus parseStatus(String value) {
        try {
            return RequestStatus.valueOf(value.toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid status: " + value);
        }
    }

    public ServiceRequestResponse acceptWork(String id, String userId) {
        // Validate input
        ValidationUtil.validateNotBlank(id, "requestId");
        ValidationUtil.validateNotBlank(userId, "userId");

        TechnicianProfileResponse technician = technicianClient.getTechnicianByUserId(userId);
        if (technician == null) {
            log.error("Technician profile not found for user: {}", userId);
            throw new NotFoundException("Technician profile not found for user: " + userId);
        }

        ServiceRequest request = fetch(id);

        // Business Rule: Only the assigned technician can accept the work
        if (request.getTechnicianId() == null || !request.getTechnicianId().equals(technician.getId())) {
            log.warn("Unauthorized access: User {} attempted to accept request not assigned to them", userId);
            throw new UnauthorizedException("You are not assigned to this service request");
        }

        // Business Rule: Work must be in ASSIGNED state to accept
        if (request.getStatus() != RequestStatus.ASSIGNED) {
            log.warn("Invalid status transition: Cannot accept request {} in status {}", id, request.getStatus());
            throw new BadRequestException(
                    "You can only accept work that is in ASSIGNED status. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.ACCEPTED);
        request.setAcceptedAt(Instant.now());

        ServiceRequest saved = requestRepository.save(request);
        log.info("Service request accepted by technician: {} for request: {}", userId, id);

        notifyCustomer(
                saved.getCustomerId(),
                "Technician Accepted Assignment",
                "Your assigned technician has accepted the service request " + saved.getRequestNumber());

        return toResponse(saved);
    }

    public ServiceRequestResponse rejectWork(String id, String userId, String reason) {
        // Validate input
        ValidationUtil.validateNotBlank(id, "requestId");
        ValidationUtil.validateNotBlank(userId, "userId");

        TechnicianProfileResponse technician = technicianClient.getTechnicianByUserId(userId);
        if (technician == null) {
            log.error("Technician profile not found for user: {}", userId);
            throw new NotFoundException("Technician profile not found for user: " + userId);
        }

        ServiceRequest request = fetch(id);

        // Business Rule: Only the assigned technician can reject the work
        if (request.getTechnicianId() == null || !request.getTechnicianId().equals(technician.getId())) {
            log.warn("Unauthorized access: User {} attempted to reject request not assigned to them", userId);
            throw new UnauthorizedException("You are not assigned to this service request");
        }

        // Business Rule: Work must be in ASSIGNED state to reject
        if (request.getStatus() != RequestStatus.ASSIGNED) {
            log.warn("Invalid status transition: Cannot reject request {} in status {}", id, request.getStatus());
            throw new BadRequestException(
                    "You can only reject work that is in ASSIGNED status. Current status: " + request.getStatus());
        }

        // Decrement technician's workload
        try {
            if (technician.getCurrentWorkload() != null && technician.getCurrentWorkload() > 0) {
                technicianClient.updateWorkload(technician.getId(), technician.getCurrentWorkload() - 1);
            }
        } catch (Exception ex) {
            log.warn("Failed to update technician workload during rejection", ex);
        }

        // Revert request back to REQUESTED state and clear technician assignment
        request.setStatus(RequestStatus.REQUESTED);
        request.setTechnicianId(null);
        request.setAssignedAt(null);

        ServiceRequest saved = requestRepository.save(request);
        log.info("Service request rejected by technician: {} for request: {}", userId, id);

        // Notify customer about rejection
        String notificationMessage = "Your service request " + saved.getRequestNumber() + " has been reassigned.";
        if (reason != null && !reason.trim().isEmpty()) {
            ValidationUtil.validateLength(reason, 500, "reason");
            notificationMessage += " Reason: " + reason;
        }

        notifyCustomer(
                saved.getCustomerId(),
                "Service Request Reassigned",
                notificationMessage);

        return toResponse(saved);
    }

    private String generateRequestNumber() {
        return "REQ-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
