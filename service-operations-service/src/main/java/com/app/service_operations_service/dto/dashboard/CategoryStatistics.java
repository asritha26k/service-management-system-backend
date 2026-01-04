package com.app.service_operations_service.dto.dashboard;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryStatistics {
    
    private String categoryId;
    private String categoryName;
    private Long totalRequests;
    @Builder.Default
    private List<ServiceInfo> services = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceInfo {
        private String serviceId;
        private String serviceName;
        private Long requestCount;
    }
}
