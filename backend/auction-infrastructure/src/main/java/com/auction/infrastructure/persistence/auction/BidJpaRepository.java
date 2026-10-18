package com.auction.infrastructure.persistence.auction;

import org.springframework.data.repository.Repository;

import java.util.UUID;

/**
 * Extends the bare {@link Repository} on purpose: no delete methods are exposed (soft delete only).
 */
interface BidJpaRepository extends Repository<BidEntity, UUID> {

    BidEntity save(BidEntity entity);
}
