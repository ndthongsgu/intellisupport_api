package com.intellisupport.domain.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Data Transfer Object containing full ticket details including conversation messages and audit history.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDetailResponse {

    private TicketResponse ticket;
    private List<TicketMessageResponse> messages;
    private List<TicketAuditLogResponse> auditLogs;
}
