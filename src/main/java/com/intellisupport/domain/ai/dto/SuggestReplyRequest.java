package com.intellisupport.domain.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request payload for AI reply suggestion with Knowledge Base context.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestReplyRequest {

    @NotNull(message = "Ticket ID is required")
    private UUID ticketId;

    private String additionalContext;
}
