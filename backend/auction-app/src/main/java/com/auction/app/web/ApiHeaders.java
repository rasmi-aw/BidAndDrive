package com.auction.app.web;

public final class ApiHeaders {

    /**
     * Identifies the calling user until real authentication exists. Not a security mechanism.
     */
    public static final String USER_ID = "X-User-Id";

    private ApiHeaders() {
    }
}
