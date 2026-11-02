package com.intellisupport.domain.ai.service;

import com.intellisupport.domain.ai.client.GeminiClient;
import com.intellisupport.domain.knowledge.entity.KnowledgeArticle;
import com.intellisupport.domain.knowledge.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Implementation of EmbeddingService integrating Google's gemini-embedding-001
 * model
 * and providing resilient deterministic offline vectorization for PostgreSQL
 * pgvector.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    public static final int VECTOR_DIMENSION = 768;

    private final GeminiClient geminiClient;
    private final KnowledgeArticleRepository articleRepository;

    @Override
    public float[] getEmbedding(String text) {
        if (!StringUtils.hasText(text)) {
            return new float[VECTOR_DIMENSION];
        }

        String cleanedText = text.trim();
        if (cleanedText.length() > 4000) {
            cleanedText = cleanedText.substring(0, 4000);
        }

        // 1. Attempt using Google Gemini Embedding Model (gemini-embedding-001)
        if (geminiClient.isConfigured()) {
            try {
                float[] apiVector = geminiClient.generateEmbedding(cleanedText);
                if (apiVector != null && apiVector.length == VECTOR_DIMENSION) {
                    return apiVector;
                }
            } catch (Exception ex) {
                log.warn("Gemini embedding generation failed, using fallback vectorizer: {}", ex.getMessage());
            }
        }

        // 2. Resilient fallback vectorizer (normalized 768-dimensional token hash
        // projection)
        return generateFallbackEmbedding(cleanedText);
    }

    @Override
    @Transactional
    public void synchronizeMissingEmbeddings() {
        try {
            List<KnowledgeArticle> missing = articleRepository.findPublishedArticlesWithoutEmbedding();
            if (missing.isEmpty()) {
                log.info("All published knowledge articles already have vector embeddings.");
                return;
            }

            log.info("Found {} published knowledge articles missing vector embeddings. Generating embeddings...",
                    missing.size());
            int count = 0;
            for (KnowledgeArticle article : missing) {
                String fullText = (article.getTitle() != null ? article.getTitle() : "") + "\n" +
                        (article.getContent() != null ? article.getContent() : "") +
                        (article.getTags() != null ? "\n" + article.getTags() : "");

                float[] vector = getEmbedding(fullText);
                articleRepository.updateArticleEmbedding(article.getId(), vector);
                count++;
            }
            log.info("Successfully generated and synchronized vector embeddings for {} knowledge articles.", count);
        } catch (Exception ex) {
            log.warn("Could not synchronize article embeddings (pgvector might not be initialized yet): {}",
                    ex.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Automatically check and vectorize any unindexed knowledge articles on startup
        synchronizeMissingEmbeddings();
    }

    /**
     * Deterministic, normalized 768-dimensional token hash projection.
     * Maps word tokens and bi-grams into the 768-dimensional hyperspace and
     * L2-normalizes,
     * allowing semantic similarity search even when offline or without Gemini API
     * key.
     */
    private float[] generateFallbackEmbedding(String text) {
        float[] vector = new float[VECTOR_DIMENSION];
        String[] tokens = text.toLowerCase().split("[\\s,;:.?!\"'()\\[\\]{}]+");

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (token.length() < 2)
                    continue;

                // 1. Single token hash
                applyTokenHash(md, token, 1.0f, vector);

                // 2. Bi-gram hash for phrasal context
                if (i + 1 < tokens.length) {
                    String bigram = token + "_" + tokens[i + 1];
                    applyTokenHash(md, bigram, 1.5f, vector);
                }
            }
        } catch (Exception ex) {
            log.debug("Fallback embedding hash error: {}", ex.getMessage());
        }

        // L2 Normalize vector
        double sumSquares = 0.0;
        for (float v : vector) {
            sumSquares += v * v;
        }

        if (sumSquares > 0) {
            float norm = (float) Math.sqrt(sumSquares);
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    private void applyTokenHash(MessageDigest md, String token, float weight, float[] vector) {
        byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
        for (int j = 0; j < 4; j++) {
            int byteIndex = j * 4;
            int intVal = ((hash[byteIndex] & 0xFF) << 24)
                    | ((hash[byteIndex + 1] & 0xFF) << 16)
                    | ((hash[byteIndex + 2] & 0xFF) << 8)
                    | (hash[byteIndex + 3] & 0xFF);

            int dim = Math.abs(intVal) % VECTOR_DIMENSION;
            float sign = ((hash[byteIndex] & 1) == 0) ? 1.0f : -1.0f;
            vector[dim] += sign * weight;
        }
    }
}
