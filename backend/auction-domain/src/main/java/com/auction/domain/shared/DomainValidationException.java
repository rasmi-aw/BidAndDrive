package com.auction.domain.shared;

/**
 * Raised when a value object or aggregate is built with invalid data.
 */
public class DomainValidationException extends DomainException {

    public DomainValidationException(String message) {
        super(message);
    }
}
