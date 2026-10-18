package com.auction.domain.user;

import java.util.Optional;

/**
 * Port for loading users; implemented by a persistence adapter.
 */
public interface UserRepository {

    Optional<User> findById(UserId id);
}
