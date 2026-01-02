package com.app.service_operations_service.controller;

import com.app.service_operations_service.service.CatalogService;
import com.app.service_operations_service.dto.catalog.*;
import com.app.service_operations_service.dto.IdMessageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public IdMessageResponse createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        ServiceCategoryResponse response = catalogService.createCategory(request);
        return new IdMessageResponse(response.getId(), "Service category created successfully");
    }

    @GetMapping("/categories")
    public List<ServiceCategoryResponse> listCategories() {
        return catalogService.listCategories();
    }

    @GetMapping("/categories/{id}")
    public ServiceCategoryResponse getCategory(@PathVariable("id") String id) {
        return catalogService.getCategoryById(id);
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public IdMessageResponse createService(@Valid @RequestBody CreateServiceItemRequest request) {
        ServiceItemResponse response = catalogService.createService(request);
        return new IdMessageResponse(response.getId(), "Service item created successfully");
    }

    @GetMapping("/services")
    public List<ServiceItemResponse> listServices() {
        return catalogService.listServices();
    }

    @GetMapping("/services/{id}")
    public ServiceItemResponse getService(@PathVariable("id") String id) {
        return catalogService.getServiceById(id);
    }
    @PutMapping("/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateService(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateServiceItemRequest request) {
        catalogService.updateService(id, request);
    }

    @GetMapping("/services/category/{categoryId}")
    public List<ServiceItemResponse> getByCategory(
            @PathVariable("categoryId") String categoryId) {
        return catalogService.listServicesByCategory(categoryId);
    }
}
