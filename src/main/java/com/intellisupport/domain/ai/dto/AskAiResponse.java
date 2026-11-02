package com.intellisupport.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response payload from AI assistant containing answer and cited references.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AskAiResponse {

    private String question;
    private String answer;
    private String modelName;
    private List<KnowledgeArticleReference> references;
}
