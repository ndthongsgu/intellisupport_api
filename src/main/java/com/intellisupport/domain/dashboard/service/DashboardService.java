package com.intellisupport.domain.dashboard.service;

import com.intellisupport.domain.dashboard.dto.DashboardStatisticsResponse;

/**
 * Service contract for calculating dashboard metrics and operational statistics.
 */
public interface DashboardService {

    DashboardStatisticsResponse getStatistics();
}
