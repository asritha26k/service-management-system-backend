package com.app.service_operations_service.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceStatistics {
    
    private String serviceId;
    private String serviceName;
    private String categoryId;
    private String categoryName;
    private Long requestCount;
}
