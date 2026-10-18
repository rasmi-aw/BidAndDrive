package com.auction.application.command;

import com.auction.domain.shared.Guard;
import com.auction.domain.user.Email;
import com.auction.domain.user.Phone;
import com.auction.domain.user.Role;
import com.auction.domain.user.UserId;

/**
 * Profile of an authenticated user as asserted by the identity provider.
 */
public record ProvisionUserCommand(UserId userId, String fullName, Email email, Phone phone, Role role) {

    public ProvisionUserCommand {
        Guard.notNull(userId, "userId");
    }
}
