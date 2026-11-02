package com.intellisupport.domain.ticket.controller;

import com.intellisupport.common.enums.TicketCategory;
import com.intellisupport.common.enums.TicketPriority;
import com.intellisupport.common.enums.TicketStatus;
import com.intellisupport.common.response.ApiResponse;
import com.intellisupport.common.response.PageResponse;
import com.intellisupport.common.util.PaginationUtils;
import com.intellisupport.domain.ticket.dto.AssignAgentRequest;
import com.intellisupport.domain.ticket.dto.CreateMessageRequest;
import com.intellisupport.domain.ticket.dto.CreateTicketRequest;
import com.intellisupport.domain.ticket.dto.TicketDetailResponse;
import com.intellisupport.domain.ticket.dto.TicketMessageResponse;
import com.intellisupport.domain.ticket.dto.TicketResponse;
import com.intellisupport.domain.ticket.dto.UpdateStatusRequest;
import com.intellisupport.domain.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing RESTful APIs for ticket management, workflows, and messages.
 */
@Tag(name = "Ticket Management", description = "Endpoints for tickets, workflows, assignment, and messages")
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "Get list of tickets", description = "Retrieves paginated tickets with optional filtering by status, priority, and category")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> getTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PaginationUtils.createPageable(page, size, sortBy, sortDir);
        PageResponse<TicketResponse> response = ticketService.getTickets(status, priority, category, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully", response));
    }

    @Operation(summary = "Create a support ticket", description = "Creates a new ticket submitted by the current authenticated customer")
    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Ticket created successfully", response));
    }

    @Operation(summary = "Get ticket details", description = "Retrieves complete ticket info, message thread, and audit log history")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getTicketById(@PathVariable UUID id) {
        TicketDetailResponse response = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success("Ticket details retrieved successfully", response));
    }

    @Operation(summary = "Update ticket status", description = "Transitions ticket to a new state based on business lifecycle rules")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        TicketResponse response = ticketService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Ticket status updated successfully", response));
    }

    @Operation(summary = "Assign ticket to an agent", description = "Assigns an agent to take ownership of a support ticket")
    @PatchMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<TicketResponse>> assignAgent(
            @PathVariable UUID id,
            @Valid @RequestBody AssignAgentRequest request) {
        TicketResponse response = ticketService.assignAgent(id, request);
        return ResponseEntity.ok(ApiResponse.success("Agent assigned successfully", response));
    }

    @Operation(summary = "Send a response message", description = "Appends a new message to the ticket conversation thread")
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMessageRequest request) {
        TicketMessageResponse response = ticketService.addMessage(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Message sent successfully", response));
    }

    @Operation(summary = "Get ticket message history", description = "Retrieves all conversation messages exchanged for a ticket")
    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<TicketMessageResponse>>> getMessages(@PathVariable UUID id) {
        List<TicketMessageResponse> response = ticketService.getMessages(id);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", response));
    }
}
