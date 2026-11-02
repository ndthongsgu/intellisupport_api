package com.intellisupport.domain.ticket.repository;

import com.intellisupport.domain.ticket.entity.TicketAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for TicketAuditLog entity data access.
 */
@Repository
public interface TicketAuditLogRepository extends JpaRepository<TicketAuditLog, UUID> {

    List<TicketAuditLog> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);
}
