package com.intellisupport.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellisupport.common.enums.ArticleStatus;
import com.intellisupport.common.enums.ErrorCode;
import com.intellisupport.common.enums.TicketCategory;
import com.intellisupport.common.enums.TicketPriority;
import com.intellisupport.common.exception.BadRequestException;
import com.intellisupport.common.exception.ResourceNotFoundException;
import com.intellisupport.domain.ai.client.GeminiClient;
import com.intellisupport.domain.ai.dto.AiReplyResponse;
import com.intellisupport.domain.ai.dto.AnalyzeTicketRequest;
import com.intellisupport.domain.ai.dto.AskAiRequest;
import com.intellisupport.domain.ai.dto.AskAiResponse;
import com.intellisupport.domain.ai.dto.KnowledgeArticleReference;
import com.intellisupport.domain.ai.dto.SuggestReplyRequest;
import com.intellisupport.domain.ai.dto.TicketAnalysisResponse;
import com.intellisupport.domain.ai.entity.AiSuggestion;
import com.intellisupport.domain.ai.repository.AiSuggestionRepository;
import com.intellisupport.domain.knowledge.entity.KnowledgeArticle;
import com.intellisupport.domain.knowledge.repository.KnowledgeArticleRepository;
import com.intellisupport.domain.ticket.entity.Ticket;
import com.intellisupport.domain.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of AiService combining Retrieval-Augmented Generation (RAG)
 * with Google Gemini LLM API and resilient offline heuristics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final TicketRepository ticketRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final AiSuggestionRepository aiSuggestionRepository;
    private final GeminiClient geminiClient;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public TicketAnalysisResponse analyzeTicket(AnalyzeTicketRequest request) {
        String subject = request.getSubject();
        String description = request.getDescription();
        Ticket ticket = null;

        if (request.getTicketId() != null) {
            ticket = ticketRepository.findById(request.getTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TICKET_NOT_FOUND,
                            "Ticket not found with id: " + request.getTicketId()));
            subject = ticket.getTitle();
            description = ticket.getDescription();
        }

        if (!StringUtils.hasText(subject) && !StringUtils.hasText(description)) {
            throw new BadRequestException("Either ticketId or both subject and description must be provided");
        }

        // =========================================================================
        // RAG Step: Vectorize ticket & retrieve matching Knowledge Base articles
        // =========================================================================
        String fullTicketText = ((subject != null ? subject : "") + "\n" + (description != null ? description : ""))
                .trim();
        List<KnowledgeArticle> relevantArticles = retrieveRelevantArticlesVector(fullTicketText, null, 3);

        TicketAnalysisResponse response = null;

        // Attempt analysis using Gemini LLM if configured, grounded in retrieved
        // articles
        if (geminiClient.isConfigured()) {
            response = callGeminiForAnalysis(subject, description, relevantArticles);
        }

        // Fallback to internal heuristic NLP if Gemini is not configured or failed
        if (response == null) {
            response = fallbackHeuristicAnalysis(subject, description);
        }

        // Save AI analysis result to ai_suggestions table if associated with a ticket
        if (ticket != null) {
            String model = geminiClient.isConfigured() ? geminiClient.getModelName() + "-rag" : "heuristic-engine";
            AiSuggestion suggestion = AiSuggestion.builder()
                    .ticket(ticket)
                    .suggestion(String.format("Category: %s | Priority: %s | Summary: %s",
                            response.getCategory(), response.getPriority(), response.getSummary()))
                    .modelName(model)
                    .build();
            aiSuggestionRepository.save(suggestion);
        }

        return response;
    }

    @Override
    @Transactional
    public AiReplyResponse suggestReply(SuggestReplyRequest request) {
        Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TICKET_NOT_FOUND,
                        "Ticket not found with id: " + request.getTicketId()));

        // =========================================================================
        // RAG Step 1: Retrieval - Vectorize ticket and find similar Knowledge Base
        // articles
        // =========================================================================
        String ticketContext = (ticket.getTitle() != null ? ticket.getTitle() : "") + "\n" +
                (ticket.getDescription() != null ? ticket.getDescription() : "");
        if (StringUtils.hasText(request.getAdditionalContext())) {
            ticketContext += "\n" + request.getAdditionalContext();
        }

        List<KnowledgeArticle> relevantArticles = retrieveRelevantArticlesVector(ticketContext, ticket.getCategory(),
                4);

        // =========================================================================
        // RAG Step 2: Augmentation - Construct contextual prompt for LLM
        // =========================================================================
        String prompt = buildRagPrompt(ticket, relevantArticles, request.getAdditionalContext());

        // =========================================================================
        // RAG Step 3: Generation - Call Gemini API or generate contextual template
        // =========================================================================
        String generatedReply = null;
        String modelName = "heuristic-rag";

        if (geminiClient.isConfigured()) {
            generatedReply = geminiClient.generateText(prompt);
            if (StringUtils.hasText(generatedReply)) {
                modelName = geminiClient.getModelName() + "-rag";
            }
        }

        if (!StringUtils.hasText(generatedReply)) {
            generatedReply = fallbackRagReply(ticket, relevantArticles);
        }

        // Record AI suggestion in database
        AiSuggestion suggestion = AiSuggestion.builder()
                .ticket(ticket)
                .suggestion(generatedReply)
                .modelName(modelName)
                .build();
        aiSuggestionRepository.save(suggestion);

        List<String> articleTitles = relevantArticles.stream()
                .map(KnowledgeArticle::getTitle)
                .toList();

        return AiReplyResponse.builder()
                .ticketId(ticket.getId())
                .suggestedReply(generatedReply)
                .modelName(modelName)
                .relevantArticles(articleTitles)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AskAiResponse askQuestion(AskAiRequest request) {
        String question = request.getQuestion().trim();

        // =========================================================================
        // RAG Step 1: Retrieval via Vector Similarity
        // =========================================================================
        List<KnowledgeArticle> relevantArticles = retrieveRelevantArticlesVector(question, request.getCategory(), 4);

        // =========================================================================
        // RAG Step 2: Augmentation & Generation
        // =========================================================================
        String prompt = buildChatRagPrompt(question, relevantArticles);
        String answer = null;
        String modelName = "heuristic-qa";

        if (geminiClient.isConfigured()) {
            answer = geminiClient.generateText(prompt);
            if (StringUtils.hasText(answer)) {
                modelName = geminiClient.getModelName() + "-rag";
            }
        }

        if (!StringUtils.hasText(answer)) {
            answer = fallbackChatAnswer(question, relevantArticles);
        }

        List<KnowledgeArticleReference> references = relevantArticles.stream()
                .map(a -> KnowledgeArticleReference.builder()
                        .id(a.getId())
                        .title(a.getTitle())
                        .category(a.getCategory())
                        .build())
                .toList();

        return AskAiResponse.builder()
                .question(question)
                .answer(answer)
                .modelName(modelName)
                .references(references)
                .build();
    }

    // =========================================================================
    // RAG Vector & Keyword Retrieval Helpers
    // =========================================================================

    /**
     * Retrieves published knowledge articles by vectorizing the input text and
     * executing
     * cosine similarity search against pgvector embeddings in PostgreSQL.
     * Gracefully augments and falls back to keyword search if vector search is
     * unavailable or returns few results.
     */
    private List<KnowledgeArticle> retrieveRelevantArticlesVector(String text, TicketCategory category, int limit) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        Set<KnowledgeArticle> articleSet = new LinkedHashSet<>();

        try {
            // 1. Generate 768-dimensional vector embedding for the input text/ticket
            float[] queryVector = embeddingService.getEmbedding(text);
            if (queryVector != null && queryVector.length > 0) {
                // 2. Query knowledge articles matching category first if category is provided
                if (category != null) {
                    List<KnowledgeArticle> categoryMatches = knowledgeArticleRepository
                            .findSimilarPublishedArticlesByCategory(queryVector, category.name(), limit);
                    articleSet.addAll(categoryMatches);
                }

                // 3. Fill remaining slots with global nearest neighbor vector matches
                if (articleSet.size() < limit) {
                    List<KnowledgeArticle> generalMatches = knowledgeArticleRepository
                            .findSimilarPublishedArticles(queryVector, limit - articleSet.size());
                    articleSet.addAll(generalMatches);
                }
            }
        } catch (Exception ex) {
            log.warn("Vector similarity search encountered an issue, falling back to keyword search: {}",
                    ex.getMessage());
        }

        // 4. Augment with keyword matching if vector search found fewer than required
        // articles
        if (articleSet.size() < limit) {
            List<KnowledgeArticle> keywordMatches = retrieveRelevantArticles(text, "", category);
            articleSet.addAll(keywordMatches);
        }

        return articleSet.stream().limit(limit).toList();
    }

    private List<KnowledgeArticle> retrieveRelevantArticles(String title, String description, TicketCategory category) {
        Set<KnowledgeArticle> articleSet = new LinkedHashSet<>();

        // Match by category first if available
        if (category != null) {
            articleSet.addAll(knowledgeArticleRepository.findByStatusAndCategory(ArticleStatus.PUBLISHED, category));
        }

        // Match by keywords in title and description
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();
        String[] keywords = combined.split("[\\s,;:.?!]+");

        for (String kw : keywords) {
            if (kw.length() > 3 && !isStopWord(kw)) {
                List<KnowledgeArticle> matches = knowledgeArticleRepository.searchPublishedArticles(kw);
                articleSet.addAll(matches);
                if (articleSet.size() >= 5) {
                    break;
                }
            }
        }

        return new ArrayList<>(articleSet);
    }

    // =========================================================================
    // Prompt Engineering Helpers
    // =========================================================================

    private String buildRagPrompt(Ticket ticket, List<KnowledgeArticle> articles, String additionalContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are IntelliSupport AI, an expert customer service assistant.\n");
        sb.append(
                "Your task is to draft a helpful, professional, and empathetic support reply for an agent to review and send to a customer.\n");
        sb.append("Ground your answer in the provided Knowledge Base documentation context below.\n\n");

        sb.append("=== KNOWLEDGE BASE CONTEXT ===\n");
        if (articles.isEmpty()) {
            sb.append("No specific documentation articles matched this inquiry.\n");
        } else {
            for (KnowledgeArticle article : articles) {
                sb.append(String.format("Article: %s\nContent:\n%s\n\n", article.getTitle(), article.getContent()));
            }
        }

        sb.append("=== TICKET DETAILS ===\n");
        sb.append("Ticket ID: #").append(ticket.getId()).append("\n");
        sb.append("Customer: ")
                .append(ticket.getCustomer() != null ? ticket.getCustomer().getFullName() : "Valued Customer")
                .append("\n");
        sb.append("Subject: ").append(ticket.getTitle()).append("\n");
        sb.append("Category: ").append(ticket.getCategory()).append("\n");
        sb.append("Priority: ").append(ticket.getPriority()).append("\n");
        sb.append("Description: ").append(ticket.getDescription()).append("\n");
        if (StringUtils.hasText(additionalContext)) {
            sb.append("Additional Context: ").append(additionalContext).append("\n");
        }

        sb.append("\nPlease output only the draft response to the customer.");
        return sb.toString();
    }

    private String buildChatRagPrompt(String question, List<KnowledgeArticle> articles) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are IntelliSupport AI Assistant.\n");
        sb.append(
                "Answer the user's question accurately and politely using the Knowledge Base articles provided below.\n");
        sb.append(
                "Cite the article titles when referencing information. If the answer cannot be found in the context, inform the user politely and offer standard advice.\n\n");

        sb.append("=== KNOWLEDGE BASE CONTEXT ===\n");
        if (articles.isEmpty()) {
            sb.append("No direct knowledge base articles found for this topic.\n");
        } else {
            for (KnowledgeArticle article : articles) {
                sb.append(String.format("Article: %s\nContent:\n%s\n\n", article.getTitle(), article.getContent()));
            }
        }

        sb.append("=== USER QUESTION ===\n");
        sb.append(question).append("\n\nAnswer:");
        return sb.toString();
    }

    // =========================================================================
    // Gemini Direct Structured Calls
    // =========================================================================

    private TicketAnalysisResponse callGeminiForAnalysis(String subject, String description,
            List<KnowledgeArticle> relevantArticles) {
        try {
            StringBuilder contextBuilder = new StringBuilder();
            if (relevantArticles != null && !relevantArticles.isEmpty()) {
                contextBuilder.append("=== RELEVANT KNOWLEDGE BASE DOCUMENTATION (RAG CONTEXT) ===\n");
                for (KnowledgeArticle article : relevantArticles) {
                    contextBuilder.append(String.format("Article: %s\nCategory: %s\nContent:\n%s\n\n",
                            article.getTitle(),
                            article.getCategory() != null ? article.getCategory().name() : "N/A",
                            article.getContent()));
                }
            }

            String prompt = String.format(
                    """
                            You are a customer support ticket analyzer for IntelliSupport.
                            Analyze the following customer ticket using the retrieved Knowledge Base documentation context below when relevant.

                            %s
                            === TICKET DETAILS ===
                            Subject: %s
                            Description: %s

                            Respond ONLY with a valid JSON object in this exact format (no markdown code blocks, just raw JSON):
                            {
                              "category": "ACCOUNT_LOGIN" | "BILLING" | "TECHNICAL_ISSUE" | "FEATURE_REQUEST" | "GENERAL_INQUIRY",
                              "priority": "LOW" | "MEDIUM" | "HIGH" | "URGENT",
                              "summary": "1-2 sentence concise summary",
                              "suggestedReply": "Initial courteous response citing knowledge base instructions if applicable"
                            }
                            """,
                    contextBuilder.toString(), subject, description);

            String responseText = geminiClient.generateText(prompt);
            if (StringUtils.hasText(responseText)) {
                // Clean potential markdown fencing ```json ... ```
                String cleanJson = responseText.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                JsonNode json = objectMapper.readTree(cleanJson);

                TicketCategory category = TicketCategory
                        .valueOf(json.path("category").asText(TicketCategory.GENERAL_INQUIRY.name()));
                TicketPriority priority = TicketPriority
                        .valueOf(json.path("priority").asText(TicketPriority.MEDIUM.name()));
                String summary = json.path("summary").asText("Customer inquiry regarding: " + subject);
                String suggestedReply = json.path("suggestedReply").asText();

                return TicketAnalysisResponse.builder()
                        .category(category)
                        .priority(priority)
                        .summary(summary)
                        .suggestedReply(suggestedReply)
                        .confidenceScore(0.96)
                        .build();
            }
        } catch (Exception ex) {
            log.warn("Failed to parse Gemini analysis output: {}", ex.getMessage());
        }
        return null;
    }

    // =========================================================================
    // Fallback Heuristics
    // =========================================================================

    private TicketAnalysisResponse fallbackHeuristicAnalysis(String subject, String description) {
        String fullContent = ((subject != null ? subject : "") + " " + (description != null ? description : ""))
                .toLowerCase();

        TicketCategory category = detectCategory(fullContent);
        TicketPriority priority = detectPriority(fullContent);
        String summary = generateSummary(subject, description);
        String suggestedReply = generateInitialReply(category, subject);

        return TicketAnalysisResponse.builder()
                .category(category)
                .priority(priority)
                .summary(summary)
                .suggestedReply(suggestedReply)
                .confidenceScore(0.85)
                .build();
    }

    private String fallbackRagReply(Ticket ticket, List<KnowledgeArticle> articles) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Hello %s,\n\nThank you for reaching out to IntelliSupport.\n\n",
                ticket.getCustomer() != null ? ticket.getCustomer().getFullName() : "Customer"));

        if (!articles.isEmpty()) {
            sb.append("Based on our knowledge base, here are the recommended steps to resolve your issue:\n\n");
            for (KnowledgeArticle article : articles) {
                sb.append(String.format("• **%s**:\n%s\n\n", article.getTitle(), article.getContent()));
            }
            sb.append("Please try these recommendations and let us know if you need any additional help.\n\n");
        } else {
            sb.append(String.format(
                    "We have received your ticket regarding '%s'. Our support team is actively reviewing your request and will follow up shortly.\n\n",
                    ticket.getTitle()));
        }

        sb.append("Best regards,\nIntelliSupport Team");
        return sb.toString();
    }

    private String fallbackChatAnswer(String question, List<KnowledgeArticle> articles) {
        if (articles.isEmpty()) {
            return "Thank you for your question. We could not find a specific article matching your inquiry in our Knowledge Base. A support agent will assist you if you create a ticket.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Here is what our Knowledge Base recommends:\n\n");
        for (KnowledgeArticle article : articles) {
            sb.append(String.format("### %s\n%s\n\n", article.getTitle(), article.getContent()));
        }
        return sb.toString();
    }

    private TicketCategory detectCategory(String content) {
        if (content.contains("login") || content.contains("password") || content.contains("account") ||
                content.contains("sign in") || content.contains("auth") || content.contains("otp")
                || content.contains("lock")) {
            return TicketCategory.ACCOUNT_LOGIN;
        } else if (content.contains("bill") || content.contains("payment") || content.contains("invoice") ||
                content.contains("charge") || content.contains("refund") || content.contains("subscription")
                || content.contains("credit card")) {
            return TicketCategory.BILLING;
        } else if (content.contains("error") || content.contains("bug") || content.contains("crash") ||
                content.contains("fail") || content.contains("exception") || content.contains("500")
                || content.contains("timeout")) {
            return TicketCategory.TECHNICAL_ISSUE;
        } else if (content.contains("feature") || content.contains("suggest") || content.contains("enhance") ||
                content.contains("request") || content.contains("idea") || content.contains("roadmap")) {
            return TicketCategory.FEATURE_REQUEST;
        }
        return TicketCategory.GENERAL_INQUIRY;
    }

    private TicketPriority detectPriority(String content) {
        if (content.contains("urgent") || content.contains("critical") || content.contains("immediately") ||
                content.contains("down") || content.contains("emergency") || content.contains("blocker")) {
            return TicketPriority.URGENT;
        } else if (content.contains("cannot login") || content.contains("broken") || content.contains("payment failed")
                ||
                content.contains("production") || content.contains("high")) {
            return TicketPriority.HIGH;
        } else if (content.contains("minor") || content.contains("cosmetic") || content.contains("typo")) {
            return TicketPriority.LOW;
        }
        return TicketPriority.MEDIUM;
    }

    private String generateSummary(String subject, String description) {
        String mainText = StringUtils.hasText(subject) ? subject : description;
        if (mainText.length() > 100) {
            return mainText.substring(0, 97) + "...";
        }
        return "Customer inquiry regarding: " + mainText;
    }

    private String generateInitialReply(TicketCategory category, String subject) {
        return switch (category) {
            case ACCOUNT_LOGIN -> "Please check our password reset guide or verify your login credentials.";
            case BILLING -> "Our finance team has received your billing inquiry and will verify the invoice details.";
            case TECHNICAL_ISSUE ->
                "Our engineering team has received your technical report and is investigating system logs.";
            case FEATURE_REQUEST ->
                "Thank you for the suggestion. We have forwarded it to our product roadmap planning.";
            case GENERAL_INQUIRY -> "Thank you for reaching out. A support representative will respond shortly.";
        };
    }

    private boolean isStopWord(String word) {
        return Set.of("this", "that", "with", "from", "have", "what", "where", "when", "your", "they", "will", "please")
                .contains(word);
    }
}
