package com.intellisupport.domain.ticket.service;

import com.intellisupport.common.enums.ErrorCode;
import com.intellisupport.common.enums.TicketCategory;
import com.intellisupport.common.enums.TicketPriority;
import com.intellisupport.common.enums.TicketStatus;
import com.intellisupport.common.enums.UserRole;
import com.intellisupport.common.exception.BadRequestException;
import com.intellisupport.common.exception.ForbiddenException;
import com.intellisupport.common.exception.InvalidStatusTransitionException;
import com.intellisupport.common.exception.ResourceNotFoundException;
import com.intellisupport.common.response.PageResponse;
import com.intellisupport.domain.ai.dto.AnalyzeTicketRequest;
import com.intellisupport.domain.ai.dto.TicketAnalysisResponse;
import com.intellisupport.domain.ai.service.AiService;
import com.intellisupport.domain.ticket.dto.AssignAgentRequest;
import com.intellisupport.domain.ticket.dto.CreateMessageRequest;
import com.intellisupport.domain.ticket.dto.CreateTicketRequest;
import com.intellisupport.domain.ticket.dto.TicketAuditLogResponse;
import com.intellisupport.domain.ticket.dto.TicketDetailResponse;
import com.intellisupport.domain.ticket.dto.TicketMessageResponse;
import com.intellisupport.domain.ticket.dto.TicketResponse;
import com.intellisupport.domain.ticket.dto.UpdateStatusRequest;
import com.intellisupport.domain.ticket.entity.Ticket;
import com.intellisupport.domain.ticket.entity.TicketAuditLog;
import com.intellisupport.domain.ticket.entity.TicketMessage;
import com.intellisupport.domain.ticket.repository.TicketAuditLogRepository;
import com.intellisupport.domain.ticket.repository.TicketMessageRepository;
import com.intellisupport.domain.ticket.repository.TicketRepository;
import com.intellisupport.domain.user.entity.User;
import com.intellisupport.domain.user.repository.UserRepository;
import com.intellisupport.domain.user.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of TicketService managing tickets, statuses, assignments, and
 * conversation messages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketAuditLogRepository ticketAuditLogRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AiService aiService;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        TicketCategory category = null;
        TicketPriority priority = null;

        try {
            TicketAnalysisResponse analysis = aiService.analyzeTicket(
                    AnalyzeTicketRequest.builder()
                            .subject(request.getTitle())
                            .description(request.getDescription())
                            .build());

            if (analysis != null) {
                if (analysis.getCategory() != null) {
                    category = analysis.getCategory();
                    log.info("AI classified ticket category as: {}", category);
                }
                if (analysis.getPriority() != null) {
                    priority = analysis.getPriority();
                    log.info("AI classified ticket priority as: {}", priority);
                }
            }
        } catch (Exception ex) {
            log.warn("AI analysis during ticket creation failed, applying fallbacks: {}", ex.getMessage());
        }

        if (category == null) {
            category = TicketCategory.GENERAL_INQUIRY;
        }
        if (priority == null) {
            priority = TicketPriority.MEDIUM;
        }

        Ticket ticket = Ticket.builder()
                .customer(currentUser)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .status(TicketStatus.OPEN)
                .priority(priority)
                .category(category)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created: id={}, customerId={}, category={}, priority={}",
                savedTicket.getId(), currentUser.getId(), category, priority);

        // Create initial audit log
        recordAuditLog(savedTicket, "CREATE_TICKET", null,
                String.format("Status: OPEN, Priority: %s, Category: %s", priority, category));

        return TicketResponse.from(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getTickets(TicketStatus status, TicketPriority priority,
            TicketCategory category, Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();

        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Customers only see their own tickets
            if (currentUser.getRole() == UserRole.CUSTOMER) {
                predicates.add(cb.equal(root.get("customer").get("id"), currentUser.getId()));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Ticket> page = ticketRepository.findAll(spec, pageable);
        return PageResponse.from(page, TicketResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketById(UUID id) {
        Ticket ticket = findTicketOrThrow(id);
        verifyTicketAccess(ticket);

        List<TicketMessageResponse> messages = ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(id)
                .stream()
                .map(TicketMessageResponse::from)
                .toList();

        List<TicketAuditLogResponse> auditLogs = ticketAuditLogRepository.findByTicketIdOrderByCreatedAtDesc(id)
                .stream()
                .map(TicketAuditLogResponse::from)
                .toList();

        return TicketDetailResponse.builder()
                .ticket(TicketResponse.from(ticket))
                .messages(messages)
                .auditLogs(auditLogs)
                .build();
    }

    @Override
    @Transactional
    public TicketResponse updateStatus(UUID id, UpdateStatusRequest request) {
        Ticket ticket = findTicketOrThrow(id);
        User currentUser = userService.getCurrentUserEntity();

        // Customers can only close their tickets
        if (currentUser.getRole() == UserRole.CUSTOMER) {
            verifyTicketAccess(ticket);
            if (request.getStatus() != TicketStatus.CLOSED) {
                throw new ForbiddenException("Customers can only close their own tickets");
            }
        }

        TicketStatus currentStatus = ticket.getStatus();
        TicketStatus targetStatus = request.getStatus();

        if (!currentStatus.canTransitionTo(targetStatus)) {
            log.warn("Illegal status transition attempt for ticket id={}: {} -> {}", id, currentStatus, targetStatus);
            throw new InvalidStatusTransitionException(currentStatus, targetStatus);
        }

        ticket.setStatus(targetStatus);
        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Ticket status updated: id={}, {} -> {}", id, currentStatus, targetStatus);

        recordAuditLog(updatedTicket, "STATUS_CHANGE", currentStatus.name(), targetStatus.name());

        return TicketResponse.from(updatedTicket);
    }

    @Override
    @Transactional
    public TicketResponse assignAgent(UUID id, AssignAgentRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        if (currentUser.getRole() == UserRole.CUSTOMER) {
            throw new ForbiddenException("Only agents or administrators can assign tickets");
        }

        Ticket ticket = findTicketOrThrow(id);
        User agent = userRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        String.format("Agent not found with id: %s", request.getAgentId())));

        if (agent.getRole() == UserRole.CUSTOMER) {
            throw new BadRequestException("Assigned user must have an AGENT or ADMIN role");
        }

        String oldAgentInfo = ticket.getAgent() != null ? ticket.getAgent().getEmail() : "UNASSIGNED";
        ticket.setAgent(agent);

        // If ticket was OPEN, transition it to IN_PROGRESS upon assignment
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            recordAuditLog(ticket, "STATUS_CHANGE", TicketStatus.OPEN.name(), TicketStatus.IN_PROGRESS.name());
        }

        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Ticket id={} assigned to agent id={}", id, agent.getId());

        recordAuditLog(updatedTicket, "ASSIGN_AGENT", oldAgentInfo, agent.getEmail());

        return TicketResponse.from(updatedTicket);
    }

    @Override
    @Transactional
    public TicketMessageResponse addMessage(UUID ticketId, CreateMessageRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);
        User currentUser = userService.getCurrentUserEntity();
        verifyTicketAccess(ticket);

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BadRequestException("Cannot post messages to a closed ticket");
        }

        TicketMessage message = TicketMessage.builder()
                .ticket(ticket)
                .sender(currentUser)
                .content(request.getContent().trim())
                .build();

        TicketMessage savedMessage = ticketMessageRepository.save(message);

        // Workflow state updates based on message sender
        if (currentUser.getRole() == UserRole.CUSTOMER && ticket.getStatus() == TicketStatus.WAITING_CUSTOMER) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
            recordAuditLog(ticket, "STATUS_CHANGE", TicketStatus.WAITING_CUSTOMER.name(),
                    TicketStatus.IN_PROGRESS.name());
        } else if (currentUser.getRole() != UserRole.CUSTOMER && ticket.getStatus() == TicketStatus.IN_PROGRESS) {
            ticket.setStatus(TicketStatus.WAITING_CUSTOMER);
            ticketRepository.save(ticket);
            recordAuditLog(ticket, "STATUS_CHANGE", TicketStatus.IN_PROGRESS.name(),
                    TicketStatus.WAITING_CUSTOMER.name());
        }

        recordAuditLog(ticket, "ADD_MESSAGE", null, String.format("Message from %s", currentUser.getEmail()));

        return TicketMessageResponse.from(savedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketMessageResponse> getMessages(UUID ticketId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        verifyTicketAccess(ticket);

        return ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(TicketMessageResponse::from)
                .toList();
    }

    private Ticket findTicketOrThrow(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TICKET_NOT_FOUND,
                        String.format("Ticket not found with id: %s", id)));
    }

    private void verifyTicketAccess(Ticket ticket) {
        User currentUser = userService.getCurrentUserEntity();
        if (currentUser.getRole() == UserRole.CUSTOMER && !ticket.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to access this ticket");
        }
    }

    private void recordAuditLog(Ticket ticket, String action, String oldValue, String newValue) {
        TicketAuditLog auditLog = TicketAuditLog.builder()
                .ticket(ticket)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        ticketAuditLogRepository.save(auditLog);
    }
}
