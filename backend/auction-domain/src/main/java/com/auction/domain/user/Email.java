package com.auction.domain.user;

import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Guard;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        value = Guard.notBlank(value, "email").toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new DomainValidationException("email is invalid: " + value);
        }
    }
}
