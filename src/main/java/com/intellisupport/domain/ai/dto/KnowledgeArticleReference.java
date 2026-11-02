package com.intellisupport.domain.ai.dto;

import com.intellisupport.common.enums.TicketCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Reference to a Knowledge Base article cited by AI in a RAG response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeArticleReference {

    private UUID id;
    private String title;
    private TicketCategory category;
}
