package com.auction.domain.user;

import java.util.Optional;

/**
 * Port for storing and loading users; implemented by a persistence adapter.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId id);
}
