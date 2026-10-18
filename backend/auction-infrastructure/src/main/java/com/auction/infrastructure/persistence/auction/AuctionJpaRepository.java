package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Extends the bare {@link Repository} on purpose: no delete methods are exposed (soft delete only).
 */
interface AuctionJpaRepository extends Repository<AuctionEntity, UUID> {

    AuctionEntity saveAndFlush(AuctionEntity entity);

    Optional<AuctionEntity> findById(UUID id);

    Page<AuctionEntity> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThan(
            AuctionStatus status, Instant startedAtOrBefore, Instant endsAfter, Pageable pageable);
}
