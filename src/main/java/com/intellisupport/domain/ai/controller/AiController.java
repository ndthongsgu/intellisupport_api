package com.intellisupport.domain.ai.controller;

import com.intellisupport.common.response.ApiResponse;
import com.intellisupport.domain.ai.dto.AiReplyResponse;
import com.intellisupport.domain.ai.dto.AnalyzeTicketRequest;
import com.intellisupport.domain.ai.dto.SuggestReplyRequest;
import com.intellisupport.domain.ai.dto.TicketAnalysisResponse;
import com.intellisupport.domain.ai.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing AI analysis and reply suggestion endpoints.
 */
@Tag(name = "AI Integration", description = "Endpoints for LLM/AI-powered ticket classification, summarization, and reply suggestion")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "Analyze ticket with AI", description = "Automatically classifies category, priority, and generates summary and initial reply suggestion")
    @PostMapping("/analyze-ticket")
    public ResponseEntity<ApiResponse<TicketAnalysisResponse>> analyzeTicket(
            @RequestBody AnalyzeTicketRequest request) {
        TicketAnalysisResponse response = aiService.analyzeTicket(request);
        return ResponseEntity.ok(ApiResponse.success("Ticket analyzed successfully", response));
    }

    @Operation(summary = "Suggest reply using AI (RAG)", description = "Generates contextual response grounded in Knowledge Base documentation")
    @PostMapping("/suggest-reply")
    public ResponseEntity<ApiResponse<AiReplyResponse>> suggestReply(
            @Valid @RequestBody SuggestReplyRequest request) {
        AiReplyResponse response = aiService.suggestReply(request);
        return ResponseEntity.ok(ApiResponse.success("Reply suggestion generated successfully", response));
    }

    @Operation(summary = "Chat with AI assistant (RAG)", description = "Answers user question by retrieving relevant Knowledge Base documentation and generating response with Gemini LLM")
    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<com.intellisupport.domain.ai.dto.AskAiResponse>> askAi(
            @Valid @RequestBody com.intellisupport.domain.ai.dto.AskAiRequest request) {
        com.intellisupport.domain.ai.dto.AskAiResponse response = aiService.askQuestion(request);
        return ResponseEntity.ok(ApiResponse.success("AI response generated successfully", response));
    }
}
