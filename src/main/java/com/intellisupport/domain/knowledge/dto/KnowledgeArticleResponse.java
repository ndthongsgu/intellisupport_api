package com.intellisupport.domain.knowledge.dto;

import com.intellisupport.common.enums.ArticleStatus;
import com.intellisupport.domain.knowledge.entity.KnowledgeArticle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a knowledge base article.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeArticleResponse {

    private UUID id;
    private String title;
    private String content;
    private com.intellisupport.common.enums.TicketCategory category;
    private String tags;
    private ArticleStatus status;
    private Boolean hasEmbedding;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KnowledgeArticleResponse from(KnowledgeArticle article) {
        if (article == null) {
            return null;
        }
        return KnowledgeArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .category(article.getCategory())
                .tags(article.getTags())
                .status(article.getStatus())
                .hasEmbedding(article.getEmbedding() != null)
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}
