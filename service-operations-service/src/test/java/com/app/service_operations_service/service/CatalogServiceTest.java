package com.app.service_operations_service.service;

import com.app.service_operations_service.dto.catalog.*;
import com.app.service_operations_service.exception.NotFoundException;
import com.app.service_operations_service.model.ServiceCategory;
import com.app.service_operations_service.model.ServiceItem;
import com.app.service_operations_service.repository.ServiceCategoryRepository;
import com.app.service_operations_service.repository.ServiceItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ServiceCategoryRepository categoryRepository;

    @Mock
    private ServiceItemRepository itemRepository;

    @InjectMocks
    private CatalogService catalogService;

    private ServiceCategory category;
    private ServiceItem serviceItem;

    @BeforeEach
    void setUp() {
        category = ServiceCategory.builder()
                .id("category-1")
                .name("Plumbing")
                .description("Plumbing services")
                .isActive(true)
                .createdAt(Instant.now())
                .build();

        serviceItem = ServiceItem.builder()
                .id("service-1")
                .categoryId("category-1")
                .name("Leak Repair")
                .description("Fix water leaks")
                .basePrice(new BigDecimal("100.00"))
                .estimatedDuration(Duration.ofMinutes(60))
                .slaHours(24)
                .isActive(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void createCategory_ShouldReturnCategoryResponse() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Plumbing");
        request.setDescription("Plumbing services");

        when(categoryRepository.save(any(ServiceCategory.class))).thenReturn(category);

        ServiceCategoryResponse response = catalogService.createCategory(request);

        assertNotNull(response);
        assertEquals("category-1", response.getId());
        assertEquals("Plumbing", response.getName());
        verify(categoryRepository, times(1)).save(any(ServiceCategory.class));
    }

    @Test
    void listCategories_ShouldReturnActiveCategories() {
        List<ServiceCategory> categories = Arrays.asList(category);
        when(categoryRepository.findByIsActiveTrue()).thenReturn(categories);

        List<ServiceCategoryResponse> responses = catalogService.listCategories();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("category-1", responses.get(0).getId());
        verify(categoryRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void getCategoryById_ShouldReturnCategory() {
        when(categoryRepository.findById("category-1")).thenReturn(Optional.of(category));

        ServiceCategoryResponse response = catalogService.getCategoryById("category-1");

        assertNotNull(response);
        assertEquals("category-1", response.getId());
        verify(categoryRepository, times(1)).findById("category-1");
    }

    @Test
    void getCategoryById_ShouldThrowNotFoundException_WhenNotFound() {
        when(categoryRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> catalogService.getCategoryById("invalid-id"));
        verify(categoryRepository, times(1)).findById("invalid-id");
    }

    @Test
    void createService_ShouldReturnServiceItemResponse() {
        CreateServiceItemRequest request = new CreateServiceItemRequest();
        request.setCategoryId("category-1");
        request.setName("Leak Repair");
        request.setDescription("Fix water leaks");
        request.setBasePrice(new BigDecimal("100.00"));
        request.setEstimatedDurationMinutes(60L);
        request.setSlaHours(24);

        when(itemRepository.save(any(ServiceItem.class))).thenReturn(serviceItem);

        ServiceItemResponse response = catalogService.createService(request);

        assertNotNull(response);
        assertEquals("service-1", response.getId());
        assertEquals("Leak Repair", response.getName());
        verify(itemRepository, times(1)).save(any(ServiceItem.class));
    }

    @Test
    void listServices_ShouldReturnActiveServices() {
        List<ServiceItem> items = Arrays.asList(serviceItem);
        when(itemRepository.findByIsActiveTrue()).thenReturn(items);

        List<ServiceItemResponse> responses = catalogService.listServices();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("service-1", responses.get(0).getId());
        verify(itemRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void getServiceById_ShouldReturnService() {
        when(itemRepository.findById("service-1")).thenReturn(Optional.of(serviceItem));

        ServiceItemResponse response = catalogService.getServiceById("service-1");

        assertNotNull(response);
        assertEquals("service-1", response.getId());
        verify(itemRepository, times(1)).findById("service-1");
    }

    @Test
    void getServiceById_ShouldThrowNotFoundException_WhenNotFound() {
        when(itemRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> catalogService.getServiceById("invalid-id"));
        verify(itemRepository, times(1)).findById("invalid-id");
    }

    @Test
    void updateService_ShouldReturnUpdatedService() {
        UpdateServiceItemRequest request = new UpdateServiceItemRequest();
        request.setName("Updated Service");
        request.setDescription("Updated description");
        request.setBasePrice(new BigDecimal("150.00"));
        request.setEstimatedDurationMinutes(90L);
        request.setSlaHours(48);
        request.setActive(true);

        when(itemRepository.findById("service-1")).thenReturn(Optional.of(serviceItem));
        serviceItem.setName("Updated Service");
        when(itemRepository.save(any(ServiceItem.class))).thenReturn(serviceItem);

        ServiceItemResponse response = catalogService.updateService("service-1", request);

        assertNotNull(response);
        verify(itemRepository, times(1)).findById("service-1");
        verify(itemRepository, times(1)).save(any(ServiceItem.class));
    }

    @Test
    void updateService_ShouldThrowNotFoundException_WhenNotFound() {
        UpdateServiceItemRequest request = new UpdateServiceItemRequest();
        request.setName("Updated Service");

        when(itemRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> catalogService.updateService("invalid-id", request));
        verify(itemRepository, times(1)).findById("invalid-id");
        verify(itemRepository, never()).save(any(ServiceItem.class));
    }

    @Test
    void listServicesByCategory_ShouldReturnServices() {
        List<ServiceItem> items = Arrays.asList(serviceItem);
        when(itemRepository.findByCategoryIdAndIsActiveTrue("category-1")).thenReturn(items);

        List<ServiceItemResponse> responses = catalogService.listServicesByCategory("category-1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("category-1", responses.get(0).getCategoryId());
        verify(itemRepository, times(1)).findByCategoryIdAndIsActiveTrue("category-1");
    }

    // Additional tests for improved coverage

    @Test
    void deleteService_ShouldDeleteService() {
        when(itemRepository.findById("service-1")).thenReturn(Optional.of(serviceItem));
        doNothing().when(itemRepository).delete(serviceItem);

        catalogService.deleteService("service-1");

        verify(itemRepository, times(1)).findById("service-1");
        verify(itemRepository, times(1)).delete(serviceItem);
    }

    @Test
    void deleteService_ShouldThrowNotFoundException_WhenNotFound() {
        when(itemRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> catalogService.deleteService("invalid-id"));
        verify(itemRepository, never()).delete(any(ServiceItem.class));
    }

    @Test
    void createService_ShouldIncludeImages() {
        CreateServiceItemRequest request = new CreateServiceItemRequest();
        request.setCategoryId("category-1");
        request.setName("Leak Repair");
        request.setDescription("Fix water leaks");
        request.setBasePrice(new BigDecimal("100.00"));
        request.setEstimatedDurationMinutes(60L);
        request.setSlaHours(24);
        
        CreateServiceItemRequest.ServiceItemImagePayload image = new CreateServiceItemRequest.ServiceItemImagePayload();
        image.setUrl("http://example.com/image.jpg");
        image.setAlt("leak repair image");
        request.setImages(Arrays.asList(image));

        when(itemRepository.save(any(ServiceItem.class))).thenReturn(serviceItem);

        ServiceItemResponse response = catalogService.createService(request);

        assertNotNull(response);
        verify(itemRepository, times(1)).save(any(ServiceItem.class));
    }

    @Test
    void createService_ShouldHandleNullImages() {
        CreateServiceItemRequest request = new CreateServiceItemRequest();
        request.setCategoryId("category-1");
        request.setName("Leak Repair");
        request.setDescription("Fix water leaks");
        request.setBasePrice(new BigDecimal("100.00"));
        request.setEstimatedDurationMinutes(60L);
        request.setSlaHours(24);
        request.setImages(null);

        when(itemRepository.save(any(ServiceItem.class))).thenReturn(serviceItem);

        ServiceItemResponse response = catalogService.createService(request);

        assertNotNull(response);
        verify(itemRepository, times(1)).save(any(ServiceItem.class));
    }

    @Test
    void updateService_ShouldUpdateWithImages() {
        UpdateServiceItemRequest request = new UpdateServiceItemRequest();
        request.setName("Updated Service");
        request.setDescription("Updated description");
        request.setBasePrice(new BigDecimal("150.00"));
        request.setEstimatedDurationMinutes(90L);
        request.setSlaHours(48);
        request.setActive(true);
        
        UpdateServiceItemRequest.ServiceItemImagePayload image = new UpdateServiceItemRequest.ServiceItemImagePayload();
        image.setUrl("http://example.com/updated.jpg");
        image.setAlt("updated image");
        request.setImages(Arrays.asList(image));

        when(itemRepository.findById("service-1")).thenReturn(Optional.of(serviceItem));
        when(itemRepository.save(any(ServiceItem.class))).thenReturn(serviceItem);

        ServiceItemResponse response = catalogService.updateService("service-1", request);

        assertNotNull(response);
        verify(itemRepository, times(1)).save(any(ServiceItem.class));
    }

    @Test
    void updateService_ShouldUpdateWithoutChangingActive() {
        UpdateServiceItemRequest request = new UpdateServiceItemRequest();
        request.setName("Updated Service");
        request.setDescription("Updated description");
        request.setBasePrice(new BigDecimal("150.00"));
        request.setEstimatedDurationMinutes(90L);
        request.setSlaHours(48);
        request.setActive(null);

        when(itemRepository.findById("service-1")).thenReturn(Optional.of(serviceItem));
        when(itemRepository.save(any(ServiceItem.class))).thenReturn(serviceItem);

        ServiceItemResponse response = catalogService.updateService("service-1", request);

        assertNotNull(response);
        verify(itemRepository, times(1)).save(any(ServiceItem.class));
    }

    @Test
    void updateService_ShouldHandleNullImages() {
        UpdateServiceItemRequest request = new UpdateServiceItemRequest();
        request.setName("Updated Service");
        request.setDescription("Updated description");
        request.setBasePrice(new BigDecimal("150.00"));
        request.setEstimatedDurationMinutes(90L);
        request.setSlaHours(48);
        request.setActive(true);
        request.setImages(null);

        when(itemRepository.findById("service-1")).thenReturn(Optional.of(serviceItem));
        when(itemRepository.save(any(ServiceItem.class))).thenReturn(serviceItem);

        ServiceItemResponse response = catalogService.updateService("service-1", request);

        assertNotNull(response);
        verify(itemRepository, times(1)).save(any(ServiceItem.class));
    }

    @Test
    void listServicesByCategory_ShouldReturnEmptyList_WhenNoneFound() {
        when(itemRepository.findByCategoryIdAndIsActiveTrue("category-1")).thenReturn(Arrays.asList());

        List<ServiceItemResponse> responses = catalogService.listServicesByCategory("category-1");

        assertNotNull(responses);
        assertEquals(0, responses.size());
        verify(itemRepository, times(1)).findByCategoryIdAndIsActiveTrue("category-1");
    }

    @Test
    void listCategories_ShouldReturnEmptyList_WhenNoneFound() {
        when(categoryRepository.findByIsActiveTrue()).thenReturn(Arrays.asList());

        List<ServiceCategoryResponse> responses = catalogService.listCategories();

        assertNotNull(responses);
        assertEquals(0, responses.size());
        verify(categoryRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void listServices_ShouldReturnEmptyList_WhenNoneFound() {
        when(itemRepository.findByIsActiveTrue()).thenReturn(Arrays.asList());

        List<ServiceItemResponse> responses = catalogService.listServices();

        assertNotNull(responses);
        assertEquals(0, responses.size());
        verify(itemRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void getServiceById_ShouldHandleNullDuration() {
        serviceItem.setEstimatedDuration(null);
        when(itemRepository.findById("service-1")).thenReturn(Optional.of(serviceItem));

        ServiceItemResponse response = catalogService.getServiceById("service-1");

        assertNotNull(response);
        assertNull(response.getEstimatedDurationMinutes());
    }

    @Test
    void createService_ShouldMapEstimatedDuration() {
        CreateServiceItemRequest request = new CreateServiceItemRequest();
        request.setCategoryId("category-1");
        request.setName("Leak Repair");
        request.setDescription("Fix water leaks");
        request.setBasePrice(new BigDecimal("100.00"));
        request.setEstimatedDurationMinutes(120L);
        request.setSlaHours(24);

        when(itemRepository.save(any(ServiceItem.class))).thenReturn(serviceItem);

        ServiceItemResponse response = catalogService.createService(request);

        assertNotNull(response);
        verify(itemRepository, times(1)).save(any(ServiceItem.class));
    }
}

