package com.intellisupport.domain.dashboard.controller;

import com.intellisupport.common.response.ApiResponse;
import com.intellisupport.domain.dashboard.dto.DashboardStatisticsResponse;
import com.intellisupport.domain.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing dashboard statistics and operational metrics.
 */
@Tag(name = "Dashboard", description = "Endpoints for platform metrics, ticket distributions, and workload statistics")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get system statistics", description = "Retrieves overview of tickets by status, priority, category, and system counts")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<DashboardStatisticsResponse>> getStatistics() {
        DashboardStatisticsResponse response = dashboardService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", response));
    }
}
