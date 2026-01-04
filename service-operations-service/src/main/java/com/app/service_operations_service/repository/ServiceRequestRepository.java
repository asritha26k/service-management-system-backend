package com.app.service_operations_service.repository;

import java.util.List;
import java.util.Optional;

import com.app.service_operations_service.model.enums.RequestStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.app.service_operations_service.model.ServiceRequest;

public interface ServiceRequestRepository extends MongoRepository<ServiceRequest, String> {
    Optional<ServiceRequest> findByRequestNumber(String requestNumber);
    List<ServiceRequest> findByCustomerId(String customerId);
    List<ServiceRequest> findByTechnicianId(String technicianId);
    List<ServiceRequest> findByStatus(RequestStatus status);
    long countByStatus(RequestStatus status);
}
