package com.app.service_operations_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.app.service_operations_service.model.TechnicianRating;

@Repository
public interface TechnicianRatingRepository extends MongoRepository<TechnicianRating, String> {
    
    List<TechnicianRating> findByTechnicianId(String technicianId);
    
    Optional<TechnicianRating> findByServiceRequestId(String serviceRequestId);
    
    List<TechnicianRating> findByCustomerId(String customerId);
    
    boolean existsByServiceRequestId(String serviceRequestId);
}
