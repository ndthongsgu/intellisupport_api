package com.intellisupport.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Result of AI reply suggestion.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiReplyResponse {

    private UUID ticketId;
    private String suggestedReply;
    private String modelName;
    private List<String> relevantArticles;
}
