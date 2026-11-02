package com.intellisupport.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Standard pagination response wrapper matching API specifications.
 *
 * @param <T> Element type
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    @Builder.Default
    private List<T> content = Collections.emptyList();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    /**
     * Converts a Spring Data Page to a PageResponse DTO.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        if (page == null) {
            return emptyPage(0, 0);
        }
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    /**
     * Converts a Spring Data Page to a PageResponse DTO and maps elements to another DTO type.
     */
    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        if (page == null) {
            return emptyPage(0, 0);
        }
        List<T> mappedContent = page.getContent().stream()
                .map(mapper)
                .toList();

        return PageResponse.<T>builder()
                .content(mappedContent)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    public static <T> PageResponse<T> emptyPage(int page, int size) {
        return PageResponse.<T>builder()
                .content(Collections.emptyList())
                .page(page)
                .size(size)
                .totalElements(0L)
                .totalPages(0)
                .first(true)
                .last(true)
                .empty(true)
                .build();
    }
}
