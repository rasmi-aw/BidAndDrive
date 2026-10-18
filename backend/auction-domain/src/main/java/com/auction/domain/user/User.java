package com.auction.domain.user;

import com.auction.domain.shared.Guard;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

/**
 * A registered platform user. Identity is the {@link UserId}; the profile only changes through
 * {@link #updateProfile}, there are no setters.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class User {

    @EqualsAndHashCode.Include
    private final UserId id;
    private String fullName;
    private Email email;
    private Phone phone;
    private Role role;
    private final Instant createdAt;
    private Instant updatedAt;

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
        return register(UserId.newId(), fullName, email, phone, role, clock);
    }

    /**
     * Registers a user whose id is assigned elsewhere (the identity provider's subject).
     */
    public static User register(UserId id, String fullName, Email email, Phone phone, Role role, Clock clock) {
        Instant now = Guard.notNull(clock, "clock").instant();
        return new User(id, fullName, email, phone, role, now, now);
    }

    /**
     * Rebuilds a user from storage.
     */
    public static User restore(UserId id, String fullName, Email email, Phone phone, Role role,
                               Instant createdAt, Instant updatedAt) {
        return new User(id, fullName, email, phone, role, createdAt, updatedAt);
    }

    /**
     * Replaces the profile with the given values; {@code updatedAt} only moves if something changed.
     *
     * @return whether anything changed
     */
    public boolean updateProfile(String fullName, Email email, Phone phone, Role role, Clock clock) {
        String newFullName = Guard.notBlank(fullName, "fullName");
        Guard.notNull(email, "email");
        Guard.notNull(phone, "phone");
        Guard.notNull(role, "role");
        Instant now = Guard.notNull(clock, "clock").instant();
        if (newFullName.equals(this.fullName) && email.equals(this.email) && phone.equals(this.phone)
                && role == this.role) {
            return false;
        }
        this.fullName = newFullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.updatedAt = now;
        return true;
    }

    public boolean isSeller() {
        return role == Role.SELLER;
    }

    public boolean isBuyer() {
        return role == Role.BUYER;
    }
}
