package com.auction.domain.shared;

/**
 * Zero-based page request.
 */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new DomainValidationException("page must not be negative: " + page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new DomainValidationException("size must be between 1 and " + MAX_SIZE + ": " + size);
        }
    }

    public long offset() {
        return (long) page * size;
    }
}
