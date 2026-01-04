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
public class CategoryStatsResponse {

    @Builder.Default
    private List<CategoryStatistics> categories = new ArrayList<>();
    private Long totalCategories;
    private Long totalRequests;
}
