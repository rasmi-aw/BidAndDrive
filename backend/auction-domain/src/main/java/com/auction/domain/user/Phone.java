package com.auction.domain.user;

import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.shared.Guard;

import java.util.regex.Pattern;

/**
 * Phone number stored without formatting: optional leading '+' followed by 8 to 15 digits.
 */
public record Phone(String value) {

    private static final Pattern FORMATTING = Pattern.compile("[\\s\\-.()]");
    private static final Pattern FORMAT = Pattern.compile("^\\+?\\d{8,15}$");

    public Phone {
        value = FORMATTING.matcher(Guard.notBlank(value, "phone")).replaceAll("");
        if (!FORMAT.matcher(value).matches()) {
            throw new DomainValidationException("phone is invalid: " + value);
        }
    }
}
