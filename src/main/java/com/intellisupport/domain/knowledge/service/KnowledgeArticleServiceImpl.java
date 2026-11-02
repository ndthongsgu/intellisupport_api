package com.intellisupport.domain.knowledge.service;

import com.intellisupport.common.enums.ArticleStatus;
import com.intellisupport.common.enums.TicketCategory;
import com.intellisupport.common.enums.UserRole;
import com.intellisupport.common.exception.ForbiddenException;
import com.intellisupport.common.response.PageResponse;
import com.intellisupport.domain.knowledge.dto.CreateArticleRequest;
import com.intellisupport.domain.knowledge.dto.ExcelImportResult;
import com.intellisupport.domain.knowledge.dto.KnowledgeArticleResponse;
import com.intellisupport.domain.knowledge.entity.KnowledgeArticle;
import com.intellisupport.domain.knowledge.repository.KnowledgeArticleRepository;
import com.intellisupport.domain.user.entity.User;
import com.intellisupport.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Implementation of KnowledgeArticleService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeArticleServiceImpl implements KnowledgeArticleService {

    private final KnowledgeArticleRepository articleRepository;
    private final UserService userService;
    private final com.intellisupport.domain.ai.service.EmbeddingService embeddingService;

    // Column indices for the Excel template
    private static final int COL_TITLE = 0;
    private static final int COL_CONTENT = 1;
    private static final int COL_CATEGORY = 2;
    private static final int COL_TAGS = 3;
    private static final int COL_STATUS = 4;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<KnowledgeArticleResponse> getArticles(ArticleStatus status, Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();

        Page<KnowledgeArticle> page;
        // Customers only see PUBLISHED articles
        if (currentUser.getRole() == UserRole.CUSTOMER) {
            page = articleRepository.findByStatus(ArticleStatus.PUBLISHED, pageable);
        } else if (status != null) {
            page = articleRepository.findByStatus(status, pageable);
        } else {
            page = articleRepository.findAll(pageable);
        }

        return PageResponse.from(page, KnowledgeArticleResponse::from);
    }

    @Override
    @Transactional
    public KnowledgeArticleResponse createArticle(CreateArticleRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        if (currentUser.getRole() == UserRole.CUSTOMER) {
            throw new ForbiddenException("Only agents or administrators can create knowledge base articles");
        }

        ArticleStatus status = request.getStatus() != null ? request.getStatus() : ArticleStatus.PUBLISHED;

        String combinedText = request.getTitle() + "\n" + request.getContent();
        if (request.getTags() != null) {
            combinedText += "\n" + request.getTags();
        }

        // Vectorize article text for semantic similarity search
        float[] embedding = embeddingService.getEmbedding(combinedText);

        KnowledgeArticle article = KnowledgeArticle.builder()
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .category(request.getCategory())
                .tags(request.getTags() != null ? request.getTags().trim() : null)
                .status(status)
                .embedding(embedding)
                .build();

        KnowledgeArticle saved = articleRepository.save(article);
        log.info("Knowledge article created: id={}, title='{}', hasEmbedding={}",
                saved.getId(), saved.getTitle(), (embedding != null));

        return KnowledgeArticleResponse.from(saved);
    }

    // ==========================================
    // Excel Template & Import
    // ==========================================

    @Override
    public ByteArrayOutputStream generateExcelTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Knowledge Articles");

            // --- Header style ---
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // --- Required marker style (red text) ---
            CellStyle requiredStyle = workbook.createCellStyle();
            Font requiredFont = workbook.createFont();
            requiredFont.setColor(IndexedColors.RED.getIndex());
            requiredFont.setItalic(true);
            requiredStyle.setFont(requiredFont);
            requiredStyle.setWrapText(true);

            // --- Sample data style ---
            CellStyle sampleStyle = workbook.createCellStyle();
            Font sampleFont = workbook.createFont();
            sampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            sampleStyle.setFont(sampleFont);
            sampleStyle.setWrapText(true);

            // --- Header row ---
            String[] headers = {"Title *", "Content *", "Category", "Tags", "Status"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- Description row (row 1) ---
            String[] descriptions = {
                    "Tiêu đề bài viết (bắt buộc, 3-255 ký tự)",
                    "Nội dung bài viết (bắt buộc)",
                    "Danh mục: " + String.join(", ", getCategoryValues()),
                    "Tags (phân cách bởi dấu phẩy)",
                    "Trạng thái: " + String.join(", ", getStatusValues()) + " (mặc định: PUBLISHED)"
            };
            Row descRow = sheet.createRow(1);
            for (int i = 0; i < descriptions.length; i++) {
                Cell cell = descRow.createCell(i);
                cell.setCellValue(descriptions[i]);
                cell.setCellStyle(requiredStyle);
            }

            // --- Sample data row (row 2) ---
            String[] sampleData = {
                    "Hướng dẫn đặt lại mật khẩu",
                    "Bước 1: Vào trang đăng nhập...\nBước 2: Nhấn 'Quên mật khẩu'...\nBước 3: Nhập email...",
                    "ACCOUNT_LOGIN",
                    "mật khẩu, đăng nhập, bảo mật",
                    "PUBLISHED"
            };
            Row sampleRow = sheet.createRow(2);
            for (int i = 0; i < sampleData.length; i++) {
                Cell cell = sampleRow.createCell(i);
                cell.setCellValue(sampleData[i]);
                cell.setCellStyle(sampleStyle);
            }

            // --- Data validation: Category dropdown (rows 3..1000) ---
            DataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);
            String[] categories = getCategoryValues();
            DataValidationConstraint categoryConstraint = dvHelper.createExplicitListConstraint(categories);
            CellRangeAddressList categoryRange = new CellRangeAddressList(2, 1000, COL_CATEGORY, COL_CATEGORY);
            DataValidation categoryValidation = dvHelper.createValidation(categoryConstraint, categoryRange);
            categoryValidation.setShowErrorBox(true);
            categoryValidation.createErrorBox("Invalid Category",
                    "Please select from: " + String.join(", ", categories));
            sheet.addValidationData(categoryValidation);

            // --- Data validation: Status dropdown (rows 3..1000) ---
            String[] statuses = getStatusValues();
            DataValidationConstraint statusConstraint = dvHelper.createExplicitListConstraint(statuses);
            CellRangeAddressList statusRange = new CellRangeAddressList(2, 1000, COL_STATUS, COL_STATUS);
            DataValidation statusValidation = dvHelper.createValidation(statusConstraint, statusRange);
            statusValidation.setShowErrorBox(true);
            statusValidation.createErrorBox("Invalid Status",
                    "Please select from: " + String.join(", ", statuses));
            sheet.addValidationData(statusValidation);

            // --- Auto-size columns ---
            sheet.setColumnWidth(COL_TITLE, 30 * 256);
            sheet.setColumnWidth(COL_CONTENT, 60 * 256);
            sheet.setColumnWidth(COL_CATEGORY, 20 * 256);
            sheet.setColumnWidth(COL_TAGS, 30 * 256);
            sheet.setColumnWidth(COL_STATUS, 15 * 256);

            // Freeze the header row
            sheet.createFreezePane(0, 1);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out;

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel template", e);
        }
    }

    @Override
    @Transactional
    public ExcelImportResult importFromExcel(MultipartFile file) {
        User currentUser = userService.getCurrentUserEntity();
        if (currentUser.getRole() == UserRole.CUSTOMER) {
            throw new ForbiddenException("Only agents or administrators can import knowledge articles");
        }

        ExcelImportResult result = ExcelImportResult.builder().build();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Determine last row with data (skip header row 0 and description row 1)
            int firstDataRow = 2;
            int lastRow = sheet.getLastRowNum();

            int totalRows = 0;
            int successCount = 0;

            for (int rowIdx = firstDataRow; rowIdx <= lastRow; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalRows++;
                int displayRow = rowIdx + 1; // 1-based row number for user display

                try {
                    // Parse required fields
                    String title = getCellStringValue(row, COL_TITLE);
                    String content = getCellStringValue(row, COL_CONTENT);
                    String categoryStr = getCellStringValue(row, COL_CATEGORY);
                    String tags = getCellStringValue(row, COL_TAGS);
                    String statusStr = getCellStringValue(row, COL_STATUS);

                    // Validate Title
                    if (title == null || title.isBlank()) {
                        result.getErrors().add(ExcelImportResult.RowError.builder()
                                .row(displayRow).field("Title").message("Title is required").build());
                        continue;
                    }
                    if (title.length() < 3 || title.length() > 255) {
                        result.getErrors().add(ExcelImportResult.RowError.builder()
                                .row(displayRow).field("Title")
                                .message("Title must be between 3 and 255 characters").build());
                        continue;
                    }

                    // Validate Content
                    if (content == null || content.isBlank()) {
                        result.getErrors().add(ExcelImportResult.RowError.builder()
                                .row(displayRow).field("Content").message("Content is required").build());
                        continue;
                    }

                    // Parse Category (optional)
                    TicketCategory category = null;
                    if (categoryStr != null && !categoryStr.isBlank()) {
                        try {
                            category = TicketCategory.valueOf(categoryStr.trim().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            result.getErrors().add(ExcelImportResult.RowError.builder()
                                    .row(displayRow).field("Category")
                                    .message("Invalid category '" + categoryStr + "'. Valid values: "
                                            + String.join(", ", getCategoryValues()))
                                    .build());
                            continue;
                        }
                    }

                    // Parse Status (optional, default PUBLISHED)
                    ArticleStatus status = ArticleStatus.PUBLISHED;
                    if (statusStr != null && !statusStr.isBlank()) {
                        try {
                            status = ArticleStatus.valueOf(statusStr.trim().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            result.getErrors().add(ExcelImportResult.RowError.builder()
                                    .row(displayRow).field("Status")
                                    .message("Invalid status '" + statusStr + "'. Valid values: "
                                            + String.join(", ", getStatusValues()))
                                    .build());
                            continue;
                        }
                    }

                    // Generate embedding
                    String combinedText = title + "\n" + content;
                    if (tags != null && !tags.isBlank()) {
                        combinedText += "\n" + tags;
                    }
                    float[] embedding = embeddingService.getEmbedding(combinedText);

                    // Build and save entity
                    KnowledgeArticle article = KnowledgeArticle.builder()
                            .title(title.trim())
                            .content(content.trim())
                            .category(category)
                            .tags(tags != null && !tags.isBlank() ? tags.trim() : null)
                            .status(status)
                            .embedding(embedding)
                            .build();

                    articleRepository.save(article);
                    successCount++;

                } catch (Exception e) {
                    log.error("Error importing row {}: {}", displayRow, e.getMessage(), e);
                    result.getErrors().add(ExcelImportResult.RowError.builder()
                            .row(displayRow).field("General")
                            .message("Unexpected error: " + e.getMessage()).build());
                }
            }

            result.setTotalRows(totalRows);
            result.setSuccessCount(successCount);
            result.setFailCount(totalRows - successCount);

            log.info("Excel import completed: total={}, success={}, fail={}",
                    totalRows, successCount, result.getFailCount());

            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read the uploaded Excel file: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    private String getCellStringValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getStringCellValue();
            default -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(row, c);
                if (value != null && !value.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String[] getCategoryValues() {
        return Arrays.stream(TicketCategory.values())
                .map(Enum::name)
                .toArray(String[]::new);
    }

    private String[] getStatusValues() {
        return Arrays.stream(ArticleStatus.values())
                .map(Enum::name)
                .toArray(String[]::new);
    }
}
