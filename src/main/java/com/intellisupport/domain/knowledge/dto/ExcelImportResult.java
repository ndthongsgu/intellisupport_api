package com.intellisupport.domain.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Result summary returned after importing knowledge articles from an Excel file.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelImportResult {

    private int totalRows;
    private int successCount;
    private int failCount;

    @Builder.Default
    private List<RowError> errors = new ArrayList<>();

    /**
     * Describes a validation or processing error for a specific row.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RowError {
        private int row;
        private String field;
        private String message;
    }
}
