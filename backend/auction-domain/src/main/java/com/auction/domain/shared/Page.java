package com.auction.domain.shared;

import java.util.List;

/**
 * One page of results, framework independent.
 */
public record Page<T>(List<T> content, int page, int size, long totalElements) {

    public Page {
        content = List.copyOf(Guard.notNull(content, "content"));
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
