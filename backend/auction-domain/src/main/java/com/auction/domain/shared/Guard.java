package com.auction.domain.shared;

/**
 * Argument checks that raise {@link DomainValidationException} instead of NPE / IAE.
 */
public final class Guard {

    private Guard() {
    }

    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new DomainValidationException(name + " is required");
        }
        return value;
    }

    public static String notBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(name + " is required");
        }
        return value.strip();
    }
}
