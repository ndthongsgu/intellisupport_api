package com.intellisupport.domain.ai.dto;

import com.intellisupport.common.enums.TicketCategory;
import com.intellisupport.common.enums.TicketPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Structured analysis result returned by AI.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAnalysisResponse {

    private TicketCategory category;
    private TicketPriority priority;
    private String summary;
    private String suggestedReply;
    private double confidenceScore;
}
