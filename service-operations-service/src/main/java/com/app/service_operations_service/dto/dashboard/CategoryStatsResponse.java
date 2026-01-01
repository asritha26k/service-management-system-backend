package com.app.service_operations_service.dto.dashboard;

import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CategoryStatsResponse {

    private Map<String, Long> serviceStatistics;
    private Long totalCategories;
}
