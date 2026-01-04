package com.app.service_operations_service.controller;

import com.app.service_operations_service.dto.dashboard.*;
import com.app.service_operations_service.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = DashboardController.class, excludeAutoConfiguration = {
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
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    private DashboardSummaryResponse summaryResponse;
    private TechnicianWorkloadResponse workloadResponse;
    private ResolutionTimeResponse resolutionTimeResponse;
    private CategoryStatsResponse categoryStatsResponse;

    @BeforeEach
    void setUp() {
        summaryResponse = new DashboardSummaryResponse();
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("REQUESTED", 5L);
        byStatus.put("ASSIGNED", 3L);
        summaryResponse.setServiceRequestsByStatus(byStatus);
        summaryResponse.setTotalActiveRequests(8L);
        summaryResponse.setTotalCompletedRequests(20L);
        summaryResponse.setTotalRevenue(new BigDecimal("5000.00"));
        summaryResponse.setMonthlyRevenue(new BigDecimal("1000.00"));
        summaryResponse.setActiveTechnicians(10);

        workloadResponse = new TechnicianWorkloadResponse();
        workloadResponse.setTotalTechnicians(10L);
        workloadResponse.setAvailableTechnicians(7L);
        workloadResponse.setAverageWorkloadRatio(0.75);

        resolutionTimeResponse = new ResolutionTimeResponse(24.5);

        categoryStatsResponse = CategoryStatsResponse.builder()
                .totalCategories(3L)
                .totalRequests(15L)
                .categories(List.of(
                        CategoryStatistics.builder()
                                .categoryId("cat-1")
                                .categoryName("HVAC Services")
                                .totalRequests(10L)
                                .services(List.of(
                                        CategoryStatistics.ServiceInfo.builder()
                                                .serviceId("service-1")
                                                .serviceName("AC Repair")
                                                .requestCount(6L)
                                                .build(),
                                        CategoryStatistics.ServiceInfo.builder()
                                                .serviceId("service-2")
                                                .serviceName("AC Installation")
                                                .requestCount(4L)
                                                .build()
                                ))
                                .build(),
                        CategoryStatistics.builder()
                                .categoryId("cat-2")
                                .categoryName("Plumbing Services")
                                .totalRequests(5L)
                                .services(List.of(
                                        CategoryStatistics.ServiceInfo.builder()
                                                .serviceId("service-3")
                                                .serviceName("Pipe Repair")
                                                .requestCount(5L)
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

    @Test
    void getSummary_ShouldReturnOk() throws Exception {
        when(dashboardService.getDashboardSummary()).thenReturn(summaryResponse);

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveRequests").value(8))
                .andExpect(jsonPath("$.totalCompletedRequests").value(20))
                .andExpect(jsonPath("$.totalRevenue").value(5000.00))
                .andExpect(jsonPath("$.activeTechnicians").value(10));
    }

    @Test
    void getTechnicianWorkload_ShouldReturnOk() throws Exception {
        when(dashboardService.getTechnicianWorkload()).thenReturn(workloadResponse);

        mockMvc.perform(get("/api/dashboard/technician-workload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTechnicians").value(10))
                .andExpect(jsonPath("$.availableTechnicians").value(7))
                .andExpect(jsonPath("$.averageWorkloadRatio").value(0.75));
    }

    @Test
    void getResolutionTime_ShouldReturnOk() throws Exception {
        when(dashboardService.getAverageResolutionTime()).thenReturn(resolutionTimeResponse);

        mockMvc.perform(get("/api/dashboard/resolution-time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageResolutionTimeHours").value(24.5));
    }

    @Test
    void getCategoryStats_ShouldReturnOk() throws Exception {
        when(dashboardService.getCategoryStatistics()).thenReturn(categoryStatsResponse);

        mockMvc.perform(get("/api/dashboard/category-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCategories").value(3))
                .andExpect(jsonPath("$.totalRequests").value(15))
                .andExpect(jsonPath("$.categories[0].categoryName").value("HVAC Services"))
                .andExpect(jsonPath("$.categories[0].totalRequests").value(10))
                .andExpect(jsonPath("$.categories[0].services[0].serviceName").value("AC Repair"))
                .andExpect(jsonPath("$.categories[0].services[0].requestCount").value(6));
    }
}

