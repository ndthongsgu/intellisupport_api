package com.intellisupport.domain.knowledge.repository;

import com.intellisupport.common.enums.ArticleStatus;
import com.intellisupport.domain.knowledge.entity.KnowledgeArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for KnowledgeArticle entity data access.
 */
@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, UUID> {

    Page<KnowledgeArticle> findByStatus(ArticleStatus status, Pageable pageable);

    @Query("SELECT a FROM KnowledgeArticle a WHERE a.status = 'PUBLISHED' AND " +
            "(LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(a.tags, '')) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<KnowledgeArticle> searchPublishedArticles(@Param("query") String query);

    List<KnowledgeArticle> findByStatusAndCategory(ArticleStatus status,
            com.intellisupport.common.enums.TicketCategory category);

    long countByStatus(ArticleStatus status);

    /**
     * Finds published knowledge articles ordered by cosine similarity with the
     * given query embedding.
     */
    @Query(value = """
            SELECT * FROM knowledge_articles
            WHERE status = 'PUBLISHED'
              AND embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:queryEmbedding AS vector) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeArticle> findSimilarPublishedArticles(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("limit") int limit);

    /**
     * Finds published knowledge articles within a specific category ordered by
     * cosine similarity.
     */
    @Query(value = """
            SELECT * FROM knowledge_articles
            WHERE status = 'PUBLISHED'
              AND embedding IS NOT NULL
              AND category = :category
            ORDER BY embedding <=> CAST(:queryEmbedding AS vector) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeArticle> findSimilarPublishedArticlesByCategory(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("category") String category,
            @Param("limit") int limit);

    /**
     * Finds all published articles that do not have an embedding computed yet.
     */
    @Query("SELECT a FROM KnowledgeArticle a WHERE a.status = 'PUBLISHED' AND a.embedding IS NULL")
    List<KnowledgeArticle> findPublishedArticlesWithoutEmbedding();

    /**
     * Updates an article's embedding directly via native query.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE knowledge_articles SET embedding = CAST(:embedding AS vector), updated_at = CURRENT_TIMESTAMP WHERE id = :id", nativeQuery = true)
    void updateArticleEmbedding(@Param("id") UUID id, @Param("embedding") float[] embedding);
}
