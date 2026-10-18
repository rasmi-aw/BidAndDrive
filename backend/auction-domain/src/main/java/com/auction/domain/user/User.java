package com.auction.domain.user;

import com.auction.domain.shared.Guard;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

/**
 * A registered platform user. Identity is the {@link UserId}; there are no setters.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class User {

    @EqualsAndHashCode.Include
    private final UserId id;
    private final String fullName;
    private final Email email;
    private final Phone phone;
    private final Role role;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(UserId id, String fullName, Email email, Phone phone, Role role, Instant createdAt, Instant updatedAt) {
        this.id = Guard.notNull(id, "id");
        this.fullName = Guard.notBlank(fullName, "fullName");
        this.email = Guard.notNull(email, "email");
        this.phone = Guard.notNull(phone, "phone");
        this.role = Guard.notNull(role, "role");
        this.createdAt = Guard.notNull(createdAt, "createdAt");
        this.updatedAt = Guard.notNull(updatedAt, "updatedAt");
    }

    public static User register(String fullName, Email email, Phone phone, Role role, Clock clock) {
        Instant now = Guard.notNull(clock, "clock").instant();
        return new User(UserId.newId(), fullName, email, phone, role, now, now);
    }

    public boolean isSeller() {
        return role == Role.SELLER;
    }

    public boolean isBuyer() {
        return role == Role.BUYER;
    }
}
