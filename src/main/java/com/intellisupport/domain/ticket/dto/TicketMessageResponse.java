package com.intellisupport.domain.ticket.dto;

import com.intellisupport.domain.ticket.entity.TicketMessage;
import com.intellisupport.domain.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a ticket conversation message.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMessageResponse {

    private UUID id;
    private UUID ticketId;
    private UserResponse sender;
    private String content;
    private LocalDateTime createdAt;

    public static TicketMessageResponse from(TicketMessage message) {
        if (message == null) {
            return null;
        }
        return TicketMessageResponse.builder()
                .id(message.getId())
                .ticketId(message.getTicket() != null ? message.getTicket().getId() : null)
                .sender(message.getSender() != null ? UserResponse.from(message.getSender()) : null)
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
