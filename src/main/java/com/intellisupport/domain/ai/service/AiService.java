package com.intellisupport.domain.ai.service;

import com.intellisupport.domain.ai.dto.AiReplyResponse;
import com.intellisupport.domain.ai.dto.AnalyzeTicketRequest;
import com.intellisupport.domain.ai.dto.SuggestReplyRequest;
import com.intellisupport.domain.ai.dto.TicketAnalysisResponse;

/**
 * Service contract for AI-driven ticket analysis, classification, and reply suggestion.
 */
public interface AiService {

    /**
     * Analyzes ticket content to extract category, priority, concise summary, and initial reply.
     */
    TicketAnalysisResponse analyzeTicket(AnalyzeTicketRequest request);

    /**
     * Generates a context-aware reply suggestion grounded in Knowledge Base documentation (RAG).
     */
    AiReplyResponse suggestReply(SuggestReplyRequest request);

    /**
     * Interactive Q&A chat with AI grounded in Knowledge Base documentation (RAG).
     *
     * @param request User question and optional category filter
     * @return Answer along with cited knowledge article references
     */
    com.intellisupport.domain.ai.dto.AskAiResponse askQuestion(com.intellisupport.domain.ai.dto.AskAiRequest request);
}
