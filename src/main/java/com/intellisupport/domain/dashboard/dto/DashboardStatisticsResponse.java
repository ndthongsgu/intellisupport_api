package com.intellisupport.domain.dashboard.dto;

import com.intellisupport.common.enums.TicketCategory;
import com.intellisupport.common.enums.TicketPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Data Transfer Object containing system overview and operational metrics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatisticsResponse {

    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long waitingCustomerTickets;
    private long resolvedTickets;
    private long closedTickets;

    private Map<TicketPriority, Long> ticketsByPriority;
    private Map<TicketCategory, Long> ticketsByCategory;

    private long totalUsers;
    private long totalArticles;
}
