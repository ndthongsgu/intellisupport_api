package com.intellisupport.domain.ticket.service;

import com.intellisupport.common.enums.TicketCategory;
import com.intellisupport.common.enums.TicketPriority;
import com.intellisupport.common.enums.TicketStatus;
import com.intellisupport.common.response.PageResponse;
import com.intellisupport.domain.ticket.dto.AssignAgentRequest;
import com.intellisupport.domain.ticket.dto.CreateMessageRequest;
import com.intellisupport.domain.ticket.dto.CreateTicketRequest;
import com.intellisupport.domain.ticket.dto.TicketDetailResponse;
import com.intellisupport.domain.ticket.dto.TicketMessageResponse;
import com.intellisupport.domain.ticket.dto.TicketResponse;
import com.intellisupport.domain.ticket.dto.UpdateStatusRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for ticket lifecycle, workflows, and messages.
 */
public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request);

    PageResponse<TicketResponse> getTickets(TicketStatus status, TicketPriority priority, TicketCategory category, Pageable pageable);

    TicketDetailResponse getTicketById(UUID id);

    TicketResponse updateStatus(UUID id, UpdateStatusRequest request);

    TicketResponse assignAgent(UUID id, AssignAgentRequest request);

    TicketMessageResponse addMessage(UUID ticketId, CreateMessageRequest request);

    List<TicketMessageResponse> getMessages(UUID ticketId);
}
