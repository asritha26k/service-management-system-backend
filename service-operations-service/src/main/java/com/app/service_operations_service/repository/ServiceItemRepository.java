package com.app.service_operations_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.app.service_operations_service.model.ServiceItem;

public interface ServiceItemRepository extends MongoRepository<ServiceItem, String> {
    List<ServiceItem> findByCategoryIdAndIsActiveTrue(String categoryId);
    List<ServiceItem> findByIsActiveTrue();
}
