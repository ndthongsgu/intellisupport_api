package com.intellisupport.domain.ai.dto;

import com.intellisupport.common.enums.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for querying the AI assistant backed by Knowledge Base RAG.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AskAiRequest {

    @NotBlank(message = "Question is required")
    private String question;

    private TicketCategory category;
}
