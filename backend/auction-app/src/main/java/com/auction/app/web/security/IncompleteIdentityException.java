package com.auction.app.web.security;

/**
 * The token is valid but lacks what the platform needs to know about the user (role, email, phone...).
 */
public class IncompleteIdentityException extends RuntimeException {

    IncompleteIdentityException(String message) {
        super(message);
    }
}
