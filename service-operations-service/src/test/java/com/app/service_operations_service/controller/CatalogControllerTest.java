package com.app.service_operations_service.controller;

import com.app.service_operations_service.dto.IdMessageResponse;
import com.app.service_operations_service.dto.catalog.*;
import com.app.service_operations_service.service.CatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CatalogController.class, excludeAutoConfiguration = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    EurekaClientAutoConfiguration.class
})
@TestPropertySource(properties = {
    "logging.level.root=INFO",
    "logging.level.com.app.service_operations_service=INFO",
    "spring.application.name=service-operations-service-test",
    "server.port=0"
})
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogService catalogService;

    @Autowired
    private ObjectMapper objectMapper;

    private ServiceCategoryResponse categoryResponse;
    private ServiceItemResponse serviceItemResponse;

    @BeforeEach
    void setUp() {
        categoryResponse = new ServiceCategoryResponse();
        categoryResponse.setId("category-1");
        categoryResponse.setName("Plumbing");
        categoryResponse.setDescription("Plumbing services");
        categoryResponse.setActive(true);
        categoryResponse.setCreatedAt(Instant.now());

        serviceItemResponse = new ServiceItemResponse();
        serviceItemResponse.setId("service-1");
        serviceItemResponse.setCategoryId("category-1");
        serviceItemResponse.setName("Leak Repair");
        serviceItemResponse.setDescription("Fix water leaks");
        serviceItemResponse.setBasePrice(new BigDecimal("100.00"));
        serviceItemResponse.setEstimatedDurationMinutes(60L);
        serviceItemResponse.setSlaHours(24);
        serviceItemResponse.setActive(true);
        serviceItemResponse.setCreatedAt(Instant.now());
    }

    @Test
    void createCategory_ShouldReturnCreated() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Plumbing");
        request.setDescription("Plumbing services");

        when(catalogService.createCategory(any(CreateCategoryRequest.class))).thenReturn(categoryResponse);

        mockMvc.perform(post("/api/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("category-1"))
                .andExpect(jsonPath("$.message").value("Service category created successfully"));
    }

    @Test
    void createCategory_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName(""); // Invalid: blank name

        mockMvc.perform(post("/api/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listCategories_ShouldReturnOk() throws Exception {
        List<ServiceCategoryResponse> categories = Arrays.asList(categoryResponse);
        when(catalogService.listCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("category-1"))
                .andExpect(jsonPath("$[0].name").value("Plumbing"));
    }

    @Test
    void getCategory_ShouldReturnOk() throws Exception {
        when(catalogService.getCategoryById("category-1")).thenReturn(categoryResponse);

        mockMvc.perform(get("/api/catalog/categories/category-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("category-1"))
                .andExpect(jsonPath("$.name").value("Plumbing"));
    }

    @Test
    void createService_ShouldReturnCreated() throws Exception {
        CreateServiceItemRequest request = new CreateServiceItemRequest();
        request.setCategoryId("category-1");
        request.setName("Leak Repair");
        request.setDescription("Fix water leaks");
        request.setBasePrice(new BigDecimal("100.00"));
        request.setEstimatedDurationMinutes(60L);
        request.setSlaHours(24);

        when(catalogService.createService(any(CreateServiceItemRequest.class))).thenReturn(serviceItemResponse);

        mockMvc.perform(post("/api/catalog/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("service-1"))
                .andExpect(jsonPath("$.message").value("Service item created successfully"));
    }

    @Test
    void createService_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        CreateServiceItemRequest request = new CreateServiceItemRequest();
        request.setCategoryId(""); // Invalid: blank categoryId

        mockMvc.perform(post("/api/catalog/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listServices_ShouldReturnOk() throws Exception {
        List<ServiceItemResponse> services = Arrays.asList(serviceItemResponse);
        when(catalogService.listServices()).thenReturn(services);

        mockMvc.perform(get("/api/catalog/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("service-1"))
                .andExpect(jsonPath("$[0].name").value("Leak Repair"));
    }

    @Test
    void getService_ShouldReturnOk() throws Exception {
        when(catalogService.getServiceById("service-1")).thenReturn(serviceItemResponse);

        mockMvc.perform(get("/api/catalog/services/service-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("service-1"))
                .andExpect(jsonPath("$.name").value("Leak Repair"));
    }

    @Test
    void updateService_ShouldReturnNoContent() throws Exception {
        UpdateServiceItemRequest request = new UpdateServiceItemRequest();
        request.setName("Updated Service");
        request.setDescription("Updated description");
        request.setBasePrice(new BigDecimal("150.00"));
        request.setEstimatedDurationMinutes(90L);
        request.setSlaHours(48);

        when(catalogService.updateService(eq("service-1"), any(UpdateServiceItemRequest.class)))
                .thenReturn(serviceItemResponse);

        mockMvc.perform(put("/api/catalog/services/service-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void getByCategory_ShouldReturnOk() throws Exception {
        List<ServiceItemResponse> services = Arrays.asList(serviceItemResponse);
        when(catalogService.listServicesByCategory("category-1")).thenReturn(services);

        mockMvc.perform(get("/api/catalog/services/category/category-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("service-1"))
                .andExpect(jsonPath("$[0].categoryId").value("category-1"));
    }
}

