package com.app.service_operations_service.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.service_operations_service.dto.requests.AcceptRejectRequest;
import com.app.service_operations_service.dto.requests.AssignRequest;
import com.app.service_operations_service.dto.requests.CreateServiceRequest;
import com.app.service_operations_service.dto.requests.ServiceRequestResponse;
import com.app.service_operations_service.dto.requests.ServiceRequestStatsResponse;
import com.app.service_operations_service.dto.requests.ServiceRequestWithCustomerResponse;
import com.app.service_operations_service.dto.requests.ServiceRequestWithTechnicianResponse;
import com.app.service_operations_service.dto.requests.UpdateStatusRequest;
import com.app.service_operations_service.exception.UnauthorizedException;
import com.app.service_operations_service.security.RequestUser;
import com.app.service_operations_service.service.ServiceRequestService;
import com.app.service_operations_service.util.ValidationUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {
    private static final String UNAUTHORIZED_USER_ID_MISSING =
            "Unauthorized: User ID not found in JWT token";
    
    private static final Logger log = LoggerFactory.getLogger(ServiceRequestController.class);

    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    // Validates that user is authenticated by checking if userId exists
    // @param user the RequestUser from JWT
    // @return validated userId
    // @throws UnauthorizedException if userId is missing
    private String validateAndGetUserId(RequestUser user) {
        String userId = user.userId();
        if (userId == null || userId.isBlank()) {
            log.warn("Request attempted without valid user ID in token");
            throw new UnauthorizedException(UNAUTHORIZED_USER_ID_MISSING);
        }
        return userId;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestResponse create(RequestUser user, @Valid @RequestBody CreateServiceRequest request) {
        String customerId = validateAndGetUserId(user);
        log.info("Creating service request for customer: {}", customerId);
        return serviceRequestService.create(request, customerId);
    }

    @GetMapping
    public List<ServiceRequestResponse> getAll() {
        log.debug("Fetching all service requests");
        return serviceRequestService.getAll();
    }

    @GetMapping("/status/{status}")
    public List<ServiceRequestResponse> getByStatus(@PathVariable("status") String status) {
        ValidationUtil.validateNotBlank(status, "status");
        log.debug("Fetching service requests by status: {}", status);
        return serviceRequestService.getByStatus(status);
    }

    @GetMapping("/{id}")
    public ServiceRequestResponse getById(@PathVariable("id") String id) {
        ValidationUtil.validateNotBlank(id, "requestId");
        log.debug("Fetching service request: {}", id);
        return serviceRequestService.getById(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<ServiceRequestResponse> getByCustomer(@PathVariable("customerId") String customerId) {
        ValidationUtil.validateNotBlank(customerId, "customerId");
        log.debug("Fetching service requests for customer: {}", customerId);
        return serviceRequestService.getByCustomer(customerId);
    }

    @GetMapping("/my-requests")
    public List<ServiceRequestResponse> getMyRequests(RequestUser user) {
        String customerId = validateAndGetUserId(user);
        log.info("Fetching service requests for authenticated customer: {}", customerId);
        return serviceRequestService.getByCustomer(customerId);
    }

    @GetMapping("/my-requests/with-technician")
    public List<ServiceRequestWithTechnicianResponse> getMyRequestsWithTechnicianDetails(RequestUser user) {
        String customerId = validateAndGetUserId(user);
        log.info("Fetching service requests with technician details for customer: {}", customerId);
        return serviceRequestService.getByCustomerWithTechnicianDetails(customerId);
    }

    @GetMapping("/customer/{customerId}/with-technician")
    public List<ServiceRequestWithTechnicianResponse> getByCustomerWithTechnicianDetails(
            @PathVariable("customerId") String customerId) {
        ValidationUtil.validateNotBlank(customerId, "customerId");
        log.debug("Fetching service requests with technician details for customer: {}", customerId);
        return serviceRequestService.getByCustomerWithTechnicianDetails(customerId);
    }

    @GetMapping("/technician/my-requests")
    public List<ServiceRequestResponse> getMyTechnicianRequests(RequestUser user) {
        String userId = validateAndGetUserId(user);
        log.info("Fetching service requests for technician: {}", userId);
        return serviceRequestService.getByTechnicianUserId(userId);
    }

    @GetMapping("/technician/my-requests/with-customer")
    public List<ServiceRequestWithCustomerResponse> getMyTechnicianRequestsWithCustomerDetails(RequestUser user) {
        String userId = validateAndGetUserId(user);
        log.info("Fetching service requests with customer details for technician: {}", userId);
        return serviceRequestService.getByTechnicianUserIdWithCustomerDetails(userId);
    }

    @PutMapping("/{id}/assign")
    public ServiceRequestResponse assign(
            @PathVariable("id") String id,
            @Valid @RequestBody AssignRequest request) {
        ValidationUtil.validateNotBlank(id, "requestId");
        log.info("Assigning technician {} to request {}", request.getTechnicianId(), id);
        return serviceRequestService.assign(id, request);
    }

    @PutMapping("/{id}/status")
    public ServiceRequestResponse updateStatus(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateStatusRequest request) {
        ValidationUtil.validateNotBlank(id, "requestId");
        log.info("Updating status of request {} to {}", id, request.getStatus());
        return serviceRequestService.updateStatus(id, request);
    }

    @PutMapping("/{id}/cancel")
    public ServiceRequestResponse cancel(@PathVariable("id") String id, RequestUser user) {
        String userId = validateAndGetUserId(user);
        ValidationUtil.validateNotBlank(id, "requestId");
        log.info("Cancelling request {} by user: {}", id, userId);
        return serviceRequestService.cancel(id, userId);
    }

    @PutMapping("/{id}/complete")
    public ServiceRequestResponse completeByTechnician(
            @PathVariable("id") String requestId,
            RequestUser user) {
        String userId = validateAndGetUserId(user);
        ValidationUtil.validateNotBlank(requestId, "requestId");
        log.info("Completing request {} by technician: {}", requestId, userId);
        return serviceRequestService.completeByTechnician(requestId, userId);
    }

    @PutMapping("/{id}/accept")
    public ServiceRequestResponse accept(
            @PathVariable("id") String id,
            RequestUser user) {
        String userId = validateAndGetUserId(user);
        ValidationUtil.validateNotBlank(id, "requestId");
        log.info("Accepting work on request {} by technician: {}", id, userId);
        return serviceRequestService.acceptWork(id, userId);
    }

    @PutMapping("/{id}/reject")
    public ServiceRequestResponse reject(
            @PathVariable("id") String id,
            @Valid @RequestBody AcceptRejectRequest request,
            RequestUser user) {
        String userId = validateAndGetUserId(user);
        ValidationUtil.validateNotBlank(id, "requestId");
        log.info("Rejecting work on request {} by technician: {}", id, userId);
        return serviceRequestService.rejectWork(id, userId, request.getReason());
    }

    @GetMapping("/stats")
    public ServiceRequestStatsResponse stats() {
        log.debug("Fetching service request statistics");
        return serviceRequestService.stats();
    }
}
