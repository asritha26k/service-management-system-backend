package com.app.service_operations_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.app.service_operations_service.model.ServiceCategory;

public interface ServiceCategoryRepository extends MongoRepository<ServiceCategory, String> {
    List<ServiceCategory> findByIsActiveTrue();
}
