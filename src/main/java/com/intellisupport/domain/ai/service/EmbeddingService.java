package com.intellisupport.domain.ai.service;

/**
 * Service contract for text embedding generation and vector conversions
 * used in RAG (Retrieval-Augmented Generation) with PostgreSQL pgvector.
 */
public interface EmbeddingService {

    /**
     * Generates a 768-dimensional float embedding vector for the provided text.
     * Uses Google's gemini-embedding-001 model via GeminiClient, or robust
     * normalized
     * semantic vectorizer when offline/unconfigured.
     *
     * @param text input text to embed
     * @return 768-dimensional float array
     */
    float[] getEmbedding(String text);

    /**
     * Automatically scans for any published KnowledgeArticle without an embedding
     * and computes & persists their vector embeddings.
     */
    void synchronizeMissingEmbeddings();
}
