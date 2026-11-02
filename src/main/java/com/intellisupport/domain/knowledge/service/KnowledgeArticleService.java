package com.intellisupport.domain.knowledge.service;

import com.intellisupport.common.enums.ArticleStatus;
import com.intellisupport.common.response.PageResponse;
import com.intellisupport.domain.knowledge.dto.CreateArticleRequest;
import com.intellisupport.domain.knowledge.dto.ExcelImportResult;
import com.intellisupport.domain.knowledge.dto.KnowledgeArticleResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;

/**
 * Service contract for knowledge base article operations.
 */
public interface KnowledgeArticleService {

    PageResponse<KnowledgeArticleResponse> getArticles(ArticleStatus status, Pageable pageable);

    KnowledgeArticleResponse createArticle(CreateArticleRequest request);

    /**
     * Generates an Excel template (.xlsx) with predefined columns and
     * data-validation drop-downs for category and status.
     */
    ByteArrayOutputStream generateExcelTemplate();

    /**
     * Parses an uploaded Excel file, validates each row, creates articles
     * (with embedding generation), and returns a summary result.
     */
    ExcelImportResult importFromExcel(MultipartFile file);
}
