package com.app.service_operations_service.controller;

import com.app.service_operations_service.dto.PagedResponse;
import com.app.service_operations_service.dto.requests.*;
import com.app.service_operations_service.model.enums.RequestStatus;
import com.app.service_operations_service.service.ServiceRequestService;
import com.app.service_operations_service.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ServiceRequestController.class, excludeAutoConfiguration = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        EurekaClientAutoConfiguration.class
})
@Import(com.app.service_operations_service.config.WebConfig.class)
@TestPropertySource(properties = {
        "logging.level.root=INFO",
        "logging.level.com.app.service_operations_service=INFO",
        "spring.application.name=service-operations-service-test",
        "server.port=0"
})
class ServiceRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceRequestService serviceRequestService;

    @Autowired
    private ObjectMapper objectMapper;

    private ServiceRequestResponse serviceRequestResponse;
    private ServiceRequestWithTechnicianResponse withTechnicianResponse;
    private ServiceRequestWithCustomerResponse withCustomerResponse;
    private ServiceRequestStatsResponse statsResponse;

    @BeforeEach
    void setUp() {
        serviceRequestResponse = new ServiceRequestResponse();
        serviceRequestResponse.setId("req-1");
        serviceRequestResponse.setRequestNumber("REQ-12345678");
        serviceRequestResponse.setCustomerId("customer-1");
        serviceRequestResponse.setServiceId("service-1");
        serviceRequestResponse.setPriority("HIGH");
        serviceRequestResponse.setStatus(RequestStatus.REQUESTED);
        serviceRequestResponse.setPreferredDate(Instant.now().plusSeconds(3600));
        serviceRequestResponse.setAddress("123 Main St");
        serviceRequestResponse.setCreatedAt(Instant.now());

        withTechnicianResponse = new ServiceRequestWithTechnicianResponse();
        withTechnicianResponse.setId("req-1");
        withTechnicianResponse.setRequestNumber("REQ-12345678");
        withTechnicianResponse.setCustomerId("customer-1");
        withTechnicianResponse.setServiceId("service-1");
        withTechnicianResponse.setStatus(RequestStatus.ASSIGNED);
        withTechnicianResponse.setTechnicianId("tech-1");

        withCustomerResponse = new ServiceRequestWithCustomerResponse();
        withCustomerResponse.setId("req-1");
        withCustomerResponse.setRequestNumber("REQ-12345678");
        withCustomerResponse.setCustomerId("customer-1");
        withCustomerResponse.setServiceId("service-1");
        withCustomerResponse.setStatus(RequestStatus.ASSIGNED);

        statsResponse = new ServiceRequestStatsResponse();
        statsResponse.setTotal(10L);
    }

    @Test
    void create_ShouldReturnCreated() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setServiceId("service-1");
        request.setPriority("HIGH");
        request.setPreferredDate(Instant.now().plusSeconds(3600));
        request.setAddress("123 Main St");

        when(serviceRequestService.create(any(CreateServiceRequest.class), eq("customer-1")))
                .thenReturn(serviceRequestResponse);

        mockMvc.perform(post("/api/service-requests")
                .header(UserContext.HEADER_USER_ID, "customer-1")
                .header(UserContext.HEADER_USER_ROLE, "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("req-1"))
                .andExpect(jsonPath("$.message").value("Service request created successfully"));
    }

    @Test
    void create_ShouldReturnForbidden_WhenUserIdMissing() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setServiceId("service-1");
        request.setPriority("HIGH");
        request.setPreferredDate(Instant.now().plusSeconds(3600));
        request.setAddress("123 Main St");

        mockMvc.perform(post("/api/service-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_ShouldReturnOk() throws Exception {
        ServiceRequestResponse req2 = new ServiceRequestResponse();
        req2.setId("req-2");
        req2.setRequestNumber("REQ-87654321");

        List<ServiceRequestResponse> requests = Arrays.asList(serviceRequestResponse, req2);
        PagedResponse<ServiceRequestResponse> pagedResponse = new PagedResponse<>(
                requests,
                0, // page number
                20, // page size
                2, // total elements
                1, // total pages
                true // is last
        );

        when(serviceRequestService.getAll(any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/service-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("req-1"))
                .andExpect(jsonPath("$.content[0].requestNumber").value("REQ-12345678"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getByStatus_ShouldReturnOk() throws Exception {
        List<ServiceRequestResponse> requests = Arrays.asList(serviceRequestResponse);
        when(serviceRequestService.getByStatus("REQUESTED")).thenReturn(requests);

        mockMvc.perform(get("/api/service-requests/status/REQUESTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));
    }

    @Test
    void getById_ShouldReturnOk() throws Exception {
        when(serviceRequestService.getById("req-1")).thenReturn(serviceRequestResponse);

        mockMvc.perform(get("/api/service-requests/req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("req-1"));
    }

    @Test
    void getByCustomer_ShouldReturnOk() throws Exception {
        List<ServiceRequestResponse> requests = Arrays.asList(serviceRequestResponse);
        when(serviceRequestService.getByCustomer("customer-1")).thenReturn(requests);

        mockMvc.perform(get("/api/service-requests/customer/customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("customer-1"));
    }

    @Test
    void getMyRequests_ShouldReturnOk() throws Exception {
        List<ServiceRequestResponse> requests = Arrays.asList(serviceRequestResponse);
        when(serviceRequestService.getByCustomer("customer-1")).thenReturn(requests);

        mockMvc.perform(get("/api/service-requests/my-requests")
                .header(UserContext.HEADER_USER_ID, "customer-1")
                .header(UserContext.HEADER_USER_ROLE, "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("customer-1"));
    }

    @Test
    void getMyRequestsWithTechnicianDetails_ShouldReturnOk() throws Exception {
        List<ServiceRequestWithTechnicianResponse> requests = Arrays.asList(withTechnicianResponse);
        when(serviceRequestService.getByCustomerWithTechnicianDetails("customer-1")).thenReturn(requests);

        mockMvc.perform(get("/api/service-requests/my-requests/with-technician")
                .header(UserContext.HEADER_USER_ID, "customer-1")
                .header(UserContext.HEADER_USER_ROLE, "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("req-1"));
    }

    @Test
    void getMyTechnicianRequests_ShouldReturnOk() throws Exception {
        List<ServiceRequestResponse> requests = Arrays.asList(serviceRequestResponse);
        when(serviceRequestService.getByTechnicianUserId("tech-user-1")).thenReturn(requests);

        mockMvc.perform(get("/api/service-requests/technician/my-requests")
                .header(UserContext.HEADER_USER_ID, "tech-user-1")
                .header(UserContext.HEADER_USER_ROLE, "TECHNICIAN"))
                .andExpect(status().isOk());
    }

    @Test
    void assign_ShouldReturnNoContent() throws Exception {
        AssignRequest request = new AssignRequest();
        request.setTechnicianId("tech-1");

        mockMvc.perform(put("/api/service-requests/req-1/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateStatus_ShouldReturnNoContent() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("ASSIGNED");

        mockMvc.perform(put("/api/service-requests/req-1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancel_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(put("/api/service-requests/req-1/cancel")
                .header(UserContext.HEADER_USER_ID, "customer-1")
                .header(UserContext.HEADER_USER_ROLE, "CUSTOMER"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reschedule_ShouldReturnNoContent() throws Exception {
        RescheduleServiceRequest request = new RescheduleServiceRequest();
        request.setPreferredDate(Instant.now().plusSeconds(7200));

        mockMvc.perform(put("/api/service-requests/req-1/reschedule")
                .header(UserContext.HEADER_USER_ID, "customer-1")
                .header(UserContext.HEADER_USER_ROLE, "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void completeByTechnician_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(put("/api/service-requests/req-1/complete")
                .header(UserContext.HEADER_USER_ID, "tech-user-1")
                .header(UserContext.HEADER_USER_ROLE, "TECHNICIAN"))
                .andExpect(status().isNoContent());
    }

    @Test
    void accept_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(put("/api/service-requests/req-1/accept")
                .header(UserContext.HEADER_USER_ID, "tech-user-1")
                .header(UserContext.HEADER_USER_ROLE, "TECHNICIAN"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reject_ShouldReturnNoContent() throws Exception {
        AcceptRejectRequest request = new AcceptRejectRequest();
        request.setAction("REJECT");
        request.setReason("Not available");

        mockMvc.perform(put("/api/service-requests/req-1/reject")
                .header(UserContext.HEADER_USER_ID, "tech-user-1")
                .header(UserContext.HEADER_USER_ROLE, "TECHNICIAN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void stats_ShouldReturnOk() throws Exception {
        when(serviceRequestService.stats()).thenReturn(statsResponse);

        mockMvc.perform(get("/api/service-requests/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));
    }

    // Additional tests for missing coverage

    @Test
    void getByCustomerWithTechnicianDetails_ShouldReturnOk() throws Exception {
        List<ServiceRequestWithTechnicianResponse> requests = Arrays.asList(withTechnicianResponse);
        when(serviceRequestService.getByCustomerWithTechnicianDetails("customer-1")).thenReturn(requests);

        mockMvc.perform(get("/api/service-requests/customer/customer-1/with-technician"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("req-1"))
                .andExpect(jsonPath("$[0].customerId").value("customer-1"));
    }

    @Test
    void getByCustomerWithTechnicianDetails_ShouldReturnEmptyList() throws Exception {
        List<ServiceRequestWithTechnicianResponse> requests = Arrays.asList();
        when(serviceRequestService.getByCustomerWithTechnicianDetails("customer-1")).thenReturn(requests);

        mockMvc.perform(get("/api/service-requests/customer/customer-1/with-technician"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void getMyTechnicianRequestsWithCustomerDetails_ShouldReturnOk() throws Exception {
        List<ServiceRequestWithCustomerResponse> requests = Arrays.asList(withCustomerResponse);
        when(serviceRequestService.getByTechnicianUserIdWithCustomerDetails("tech-user-1")).thenReturn(requests);

        mockMvc.perform(get("/api/service-requests/technician/my-requests/with-customer")
                .header(UserContext.HEADER_USER_ID, "tech-user-1")
                .header(UserContext.HEADER_USER_ROLE, "TECHNICIAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("req-1"))
                .andExpect(jsonPath("$[0].customerId").value("customer-1"));
    }

    @Test
    void getMyTechnicianRequestsWithCustomerDetails_ShouldReturnEmptyList() throws Exception {
        List<ServiceRequestWithCustomerResponse> requests = Arrays.asList();
        when(serviceRequestService.getByTechnicianUserIdWithCustomerDetails("tech-user-1")).thenReturn(requests);

        mockMvc.perform(get("/api/service-requests/technician/my-requests/with-customer")
                .header(UserContext.HEADER_USER_ID, "tech-user-1")
                .header(UserContext.HEADER_USER_ROLE, "TECHNICIAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void getMyTechnicianRequestsWithCustomerDetails_ShouldReturnForbidden_WhenUserIdMissing() throws Exception {
        mockMvc.perform(get("/api/service-requests/technician/my-requests/with-customer")
                .header(UserContext.HEADER_USER_ROLE, "TECHNICIAN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByCustomerWithTechnicianDetails_ShouldReturnMultipleRequests() throws Exception {
        ServiceRequestWithTechnicianResponse response2 = new ServiceRequestWithTechnicianResponse();
        response2.setId("req-2");
        response2.setRequestNumber("REQ-87654321");
        response2.setCustomerId("customer-1");
        response2.setServiceId("service-2");
        response2.setStatus(RequestStatus.IN_PROGRESS);
        response2.setTechnicianId("tech-2");

        List<ServiceRequestWithTechnicianResponse> requests = Arrays.asList(withTechnicianResponse, response2);
        when(serviceRequestService.getByCustomerWithTechnicianDetails("customer-1")).thenReturn(requests);

        mockMvc.perform(get("/api/service-requests/customer/customer-1/with-technician"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("req-1"))
                .andExpect(jsonPath("$[1].id").value("req-2"));
    }

    @Test
    void getMyTechnicianRequestsWithCustomerDetails_ShouldReturnMultipleRequests() throws Exception {
        ServiceRequestWithCustomerResponse response2 = new ServiceRequestWithCustomerResponse();
        response2.setId("req-2");
        response2.setRequestNumber("REQ-87654321");
        response2.setCustomerId("customer-2");
        response2.setServiceId("service-2");
        response2.setStatus(RequestStatus.IN_PROGRESS);

        List<ServiceRequestWithCustomerResponse> requests = Arrays.asList(withCustomerResponse, response2);
        when(serviceRequestService.getByTechnicianUserIdWithCustomerDetails("tech-user-1")).thenReturn(requests);

        mockMvc.perform(get("/api/service-requests/technician/my-requests/with-customer")
                .header(UserContext.HEADER_USER_ID, "tech-user-1")
                .header(UserContext.HEADER_USER_ROLE, "TECHNICIAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("req-1"))
                .andExpect(jsonPath("$[1].id").value("req-2"));
    }
}
