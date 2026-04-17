package com.example.sams.common.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private PaginationUtils() {
    }

    public static Pageable buildPageable(Integer page, Integer size, String sortBy, String direction) {
        int normalizedPage = page == null ? DEFAULT_PAGE : Math.max(page, 0);
        int normalizedSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        String normalizedSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();

        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(normalizedSortBy).descending()
                : Sort.by(normalizedSortBy).ascending();

        return PageRequest.of(normalizedPage, normalizedSize, sort);
    }
}
