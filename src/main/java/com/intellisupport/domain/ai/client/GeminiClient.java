package com.intellisupport.domain.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Client for interacting with Google Gemini LLM API via REST.
 */
@Slf4j
@Component
public class GeminiClient {

    private final String apiKey;
    private final String model;
    private final String embeddingModel;
    private final String baseUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.embedding-model}") String embeddingModel,
            @Value("${gemini.base-url}") String baseUrl) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null ? model.trim() : "gemini-3.5-flash-lite";
        this.embeddingModel = embeddingModel != null ? embeddingModel.trim() : "gemini-embedding-001";
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "https://generativelanguage.googleapis.com/v1beta/models";
        this.restClient = RestClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Checks if Gemini API key has been provided in application properties or
     * environment variables.
     */
    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    public String getModelName() {
        return model;
    }

    public String getEmbeddingModelName() {
        return embeddingModel;
    }

    /**
     * Sends prompt to Gemini API and returns the generated text.
     * Returns null if API call fails or key is not configured.
     *
     * @param prompt User or RAG contextual prompt
     * @return Generated response text from Gemini
     */
    public String generateText(String prompt) {
        if (!isConfigured()) {
            log.debug("Gemini API key is not configured; using offline heuristic engine");
            return null;
        }

        try {
            String url = String.format("%s/%s:generateContent?key=%s", baseUrl, model, apiKey);

            Map<String, Object> payload = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)))),
                    "generationConfig", Map.of(
                            "temperature", 0.3,
                            "maxOutputTokens", 2048));

            String responseBody = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (responseBody != null) {
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        return parts.get(0).path("text").asText();
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to generate content from Gemini API: {}", ex.getMessage());
        }

        return null;
    }

    /**
     * Generates a 768-dimensional float embedding vector for the provided text
     * using Google's gemini-embedding-001 model.
     *
     * @param text The input text to embed
     * @return float array of embedding values, or null if unconfigured/failed
     */
    public float[] generateEmbedding(String text) {
        if (!isConfigured() || !StringUtils.hasText(text)) {
            log.debug("Gemini API key is not configured or text is blank; cannot generate embedding via API");
            return null;
        }

        try {
            String url = String.format("%s/%s:embedContent?key=%s", baseUrl, embeddingModel, apiKey);

            Map<String, Object> payload = Map.of(
                    "model", "models/" + embeddingModel,
                    "content", Map.of(
                            "parts", List.of(
                                    Map.of("text", text.trim()))));

            String responseBody = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (responseBody != null) {
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode values = root.path("embedding").path("values");
                if (values.isArray() && !values.isEmpty()) {
                    float[] vector = new float[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        vector[i] = (float) values.get(i).asDouble();
                    }
                    return vector;
                }
            }
        } catch (Exception ex) {
            log.error("Failed to generate embedding from Gemini API (model: {}): {}", embeddingModel, ex.getMessage());
        }

        return null;
    }
}
