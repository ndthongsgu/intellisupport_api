package com.intellisupport.domain.knowledge.controller;

import com.intellisupport.common.enums.ArticleStatus;
import com.intellisupport.common.response.ApiResponse;
import com.intellisupport.common.response.PageResponse;
import com.intellisupport.common.util.PaginationUtils;
import com.intellisupport.domain.knowledge.dto.CreateArticleRequest;
import com.intellisupport.domain.knowledge.dto.ExcelImportResult;
import com.intellisupport.domain.knowledge.dto.KnowledgeArticleResponse;
import com.intellisupport.domain.knowledge.service.KnowledgeArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;

/**
 * Controller exposing endpoints for knowledge base articles.
 */
@Tag(name = "Knowledge Base", description = "Endpoints for managing and querying documentation articles")
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeArticleController {

    private final KnowledgeArticleService articleService;

    @Operation(summary = "Get knowledge articles", description = "Retrieves a paginated list of knowledge base articles")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<KnowledgeArticleResponse>>> getArticles(
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PaginationUtils.createPageable(page, size, sortBy, sortDir);
        PageResponse<KnowledgeArticleResponse> response = articleService.getArticles(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Articles retrieved successfully", response));
    }

    @Operation(summary = "Create knowledge article", description = "Creates a new article (restricted to AGENT and ADMIN)")
    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgeArticleResponse>> createArticle(
            @Valid @RequestBody CreateArticleRequest request) {
        KnowledgeArticleResponse response = articleService.createArticle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Article created successfully", response));
    }

    @Operation(summary = "Download Excel template",
            description = "Downloads an .xlsx template file with predefined columns and data-validation dropdowns for importing knowledge articles")
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        ByteArrayOutputStream out = articleService.generateExcelTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "knowledge_articles_template.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(out.toByteArray());
    }

    @Operation(summary = "Import knowledge articles from Excel",
            description = "Uploads an .xlsx file to batch-create knowledge articles. "
                    + "Each row is validated individually; successfully parsed rows are saved with auto-generated embeddings. "
                    + "Returns a summary with success/fail counts and per-row error details.")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExcelImportResult>> importFromExcel(
            @RequestParam("file") MultipartFile file) {

        // Basic file validation
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.success("Uploaded file is empty", null));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.success("Only Excel files (.xlsx, .xls) are supported", null));
        }

        ExcelImportResult result = articleService.importFromExcel(file);

        String message = String.format("Import completed: %d/%d articles created successfully",
                result.getSuccessCount(), result.getTotalRows());

        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}
