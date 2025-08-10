package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PagedResponse", description = "Generic pagination wrapper")
/**
 * Универсальная обертка ответа с пагинацией.
 */
public class PagedResponse<T> {
    /** Содержимое страницы. */
    @Schema(description = "Page content")
    private List<T> content;
    /** Номер текущей страницы (0-индексация). */
    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;
    /** Запрошенный размер страницы. */
    @Schema(description = "Requested page size", example = "20")
    private int size;
    /** Общее количество элементов. */
    @Schema(description = "Total number of elements", example = "125")
    private long totalElements;
    /** Общее число страниц. */
    @Schema(description = "Total pages count", example = "7")
    private int totalPages;
}
