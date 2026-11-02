package com.intellisupport.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility functions for safely handling pagination and sorting parameters.
 */
public final class PaginationUtils {

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
        // Prevent instantiation
    }

    /**
     * Builds a safe Pageable instance with boundary checks on page and size.
     *
     * @param page    0-based page index (defaults to 0 if negative)
     * @param size    number of items per page (bounded between 1 and MAX_PAGE_SIZE)
     * @param sortBy  property name to sort by (defaults to "id" if blank)
     * @param sortDir sort direction ("asc" or "desc", defaults to "desc")
     */
    public static Pageable createPageable(Integer page, Integer size, String sortBy, String sortDir) {
        int safePage = (page == null || page < 0) ? DEFAULT_PAGE_NUMBER : page;
        int safeSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        String property = (sortBy == null || sortBy.trim().isEmpty()) ? "id" : sortBy.trim();
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }

    /**
     * Builds a safe Pageable instance using an existing Sort definition.
     */
    public static Pageable createPageable(Integer page, Integer size, Sort sort) {
        int safePage = (page == null || page < 0) ? DEFAULT_PAGE_NUMBER : page;
        int safeSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Sort safeSort = sort != null ? sort : Sort.by(Sort.Direction.DESC, "id");

        return PageRequest.of(safePage, safeSize, safeSort);
    }
}
