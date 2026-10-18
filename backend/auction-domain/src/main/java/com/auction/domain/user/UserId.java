package com.auction.domain.user;

import com.auction.domain.shared.Guard;

import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Guard.notNull(value, "userId");
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}
