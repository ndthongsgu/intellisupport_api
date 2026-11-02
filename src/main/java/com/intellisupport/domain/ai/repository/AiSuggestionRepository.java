package com.intellisupport.domain.ai.repository;

import com.intellisupport.domain.ai.entity.AiSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for AiSuggestion entity data access.
 */
@Repository
public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {

    List<AiSuggestion> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);
}
