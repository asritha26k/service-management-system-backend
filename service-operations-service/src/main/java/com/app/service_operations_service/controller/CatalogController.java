package com.app.service_operations_service.controller;

import com.app.service_operations_service.service.CatalogService;
import com.app.service_operations_service.dto.catalog.*;
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
    public ServiceCategoryResponse createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return catalogService.createCategory(request);
    }

    @GetMapping("/categories")
    public List<ServiceCategoryResponse> listCategories() {
        return catalogService.listCategories();
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceItemResponse createService(@Valid @RequestBody CreateServiceItemRequest request) {
        return catalogService.createService(request);
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
    public ServiceItemResponse updateService(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateServiceItemRequest request) {
        return catalogService.updateService(id, request);
    }

    @GetMapping("/services/category/{categoryId}")
    public List<ServiceItemResponse> getByCategory(
            @PathVariable("categoryId") String categoryId) {
        return catalogService.listServicesByCategory(categoryId);
    }
}
