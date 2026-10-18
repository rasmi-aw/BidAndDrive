package com.auction.infrastructure.persistence.user;

import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Extends the bare {@link Repository} on purpose: no delete methods are exposed (soft delete only).
 */
interface UserJpaRepository extends Repository<UserEntity, UUID> {

    UserEntity save(UserEntity entity);

    Optional<UserEntity> findById(UUID id);
}
