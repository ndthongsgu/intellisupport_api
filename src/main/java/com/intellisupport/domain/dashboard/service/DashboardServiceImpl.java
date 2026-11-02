package com.intellisupport.domain.dashboard.service;

import com.intellisupport.common.enums.ArticleStatus;
import com.intellisupport.common.enums.TicketCategory;
import com.intellisupport.common.enums.TicketPriority;
import com.intellisupport.common.enums.TicketStatus;
import com.intellisupport.domain.dashboard.dto.DashboardStatisticsResponse;
import com.intellisupport.domain.knowledge.repository.KnowledgeArticleRepository;
import com.intellisupport.domain.ticket.repository.TicketRepository;
import com.intellisupport.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

/**
 * Implementation of DashboardService aggregating system metrics.
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final KnowledgeArticleRepository articleRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatisticsResponse getStatistics() {
        long totalTickets = ticketRepository.count();
        long openTickets = ticketRepository.countByStatus(TicketStatus.OPEN);
        long inProgressTickets = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long waitingCustomerTickets = ticketRepository.countByStatus(TicketStatus.WAITING_CUSTOMER);
        long resolvedTickets = ticketRepository.countByStatus(TicketStatus.RESOLVED);
        long closedTickets = ticketRepository.countByStatus(TicketStatus.CLOSED);

        Map<TicketPriority, Long> priorityMap = new EnumMap<>(TicketPriority.class);
        for (TicketPriority priority : TicketPriority.values()) {
            priorityMap.put(priority, ticketRepository.countByPriority(priority));
        }

        Map<TicketCategory, Long> categoryMap = new EnumMap<>(TicketCategory.class);
        for (TicketCategory category : TicketCategory.values()) {
            categoryMap.put(category, ticketRepository.countByCategory(category));
        }

        long totalUsers = userRepository.count();
        long totalArticles = articleRepository.countByStatus(ArticleStatus.PUBLISHED);

        return DashboardStatisticsResponse.builder()
                .totalTickets(totalTickets)
                .openTickets(openTickets)
                .inProgressTickets(inProgressTickets)
                .waitingCustomerTickets(waitingCustomerTickets)
                .resolvedTickets(resolvedTickets)
                .closedTickets(closedTickets)
                .ticketsByPriority(priorityMap)
                .ticketsByCategory(categoryMap)
                .totalUsers(totalUsers)
                .totalArticles(totalArticles)
                .build();
    }
}
