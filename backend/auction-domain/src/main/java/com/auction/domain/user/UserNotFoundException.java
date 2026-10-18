package com.auction.domain.user;

import com.auction.domain.shared.DomainException;

public class UserNotFoundException extends DomainException {

    public UserNotFoundException(UserId userId) {
        super("user " + userId.value() + " not found");
    }
}
