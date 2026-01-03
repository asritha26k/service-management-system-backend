package com.app.service_operations_service.service;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.service_operations_service.dto.catalog.CreateCategoryRequest;
import com.app.service_operations_service.dto.catalog.CreateServiceItemRequest;
import com.app.service_operations_service.dto.catalog.ServiceCategoryResponse;
import com.app.service_operations_service.dto.catalog.ServiceItemResponse;
import com.app.service_operations_service.dto.catalog.UpdateServiceItemRequest;
import com.app.service_operations_service.exception.NotFoundException;
import com.app.service_operations_service.model.ServiceCategory;
import com.app.service_operations_service.model.ServiceItem;
import com.app.service_operations_service.repository.ServiceCategoryRepository;
import com.app.service_operations_service.repository.ServiceItemRepository;

@Service
@Transactional
public class CatalogService {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceItemRepository itemRepository;

    public CatalogService(ServiceCategoryRepository categoryRepository, ServiceItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    public ServiceCategoryResponse createCategory(CreateCategoryRequest request) {
        ServiceCategory category = new ServiceCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        ServiceCategory saved = categoryRepository.save(category);
        return toCategoryResponse(saved);
    }

    public List<ServiceCategoryResponse> listCategories() {
        return categoryRepository.findByIsActiveTrue().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    public ServiceCategoryResponse getCategoryById(String id) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service category not found: " + id));
        return toCategoryResponse(category);
    }

    public ServiceItemResponse createService(CreateServiceItemRequest request) {
        ServiceItem item = new ServiceItem();
        item.setCategoryId(request.getCategoryId());
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setBasePrice(request.getBasePrice());
        item.setEstimatedDuration(Duration.ofMinutes(request.getEstimatedDurationMinutes()));
        item.setSlaHours(request.getSlaHours());
        if (request.getImages() != null) {
            item.setImages(request.getImages().stream()
                    .map(img -> new ServiceItem.ServiceItemImage(img.getUrl(), img.getAlt()))
                    .toList());
        }
        ServiceItem saved = itemRepository.save(item);
        return toItemResponse(saved);
    }

    public List<ServiceItemResponse> listServices() {
        return itemRepository.findByIsActiveTrue().stream()
                .map(this::toItemResponse)
                .toList();
    }

    public ServiceItemResponse getServiceById(String id) {
        ServiceItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service item not found: " + id));
        return toItemResponse(item);
    }

    public ServiceItemResponse updateService(String id, UpdateServiceItemRequest request) {
        ServiceItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service item not found: " + id));
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setBasePrice(request.getBasePrice());
        item.setEstimatedDuration(Duration.ofMinutes(request.getEstimatedDurationMinutes()));
        item.setSlaHours(request.getSlaHours());
        if (request.getActive() != null) {
            item.setActive(request.getActive());
        }
        if (request.getImages() != null) {
            item.setImages(request.getImages().stream()
                    .map(img -> new ServiceItem.ServiceItemImage(img.getUrl(), img.getAlt()))
                    .toList());
        }
        return toItemResponse(itemRepository.save(item));
    }

    public List<ServiceItemResponse> listServicesByCategory(String categoryId) {
        return itemRepository.findByCategoryIdAndIsActiveTrue(categoryId).stream()
                .map(this::toItemResponse)
                .toList();
    }

    private ServiceCategoryResponse toCategoryResponse(ServiceCategory category) {
        ServiceCategoryResponse response = new ServiceCategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setActive(category.isActive());
        response.setCreatedAt(category.getCreatedAt());
        return response;
    }

    private ServiceItemResponse toItemResponse(ServiceItem item) {
        ServiceItemResponse response = new ServiceItemResponse();
        response.setId(item.getId());
        response.setCategoryId(item.getCategoryId());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setBasePrice(item.getBasePrice());
        response.setEstimatedDurationMinutes(item.getEstimatedDuration() != null
                ? item.getEstimatedDuration().toMinutes() : null);
        response.setSlaHours(item.getSlaHours());
        if (item.getImages() != null) {
            response.setImages(item.getImages().stream()
                    .map(img -> {
                        ServiceItemResponse.ServiceItemImagePayload payload = new ServiceItemResponse.ServiceItemImagePayload();
                        payload.setUrl(img.getUrl());
                        payload.setAlt(img.getAlt());
                        return payload;
                    }).toList());
        }
        response.setActive(item.isActive());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }
}
