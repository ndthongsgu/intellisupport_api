package com.intellisupport.domain.ticket.dto;

import com.intellisupport.domain.ticket.entity.TicketAuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a ticket audit log entry.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAuditLogResponse {

    private UUID id;
    private UUID ticketId;
    private String action;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;

    public static TicketAuditLogResponse from(TicketAuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return TicketAuditLogResponse.builder()
                .id(auditLog.getId())
                .ticketId(auditLog.getTicket() != null ? auditLog.getTicket().getId() : null)
                .action(auditLog.getAction())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
