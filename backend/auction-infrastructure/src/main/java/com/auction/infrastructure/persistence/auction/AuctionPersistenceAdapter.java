package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionConcurrentlyModifiedException;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AuctionPersistenceAdapter implements AuctionRepository {

    private final AuctionJpaRepository jpaRepository;

    /**
     * Optimistic locking in two places: the stored version must still be the one the domain object was loaded at
     * (catches stale copies), and Hibernate's {@code UPDATE ... WHERE version = ?} catches a concurrent commit
     * between that check and the flush. Both surface as {@link AuctionConcurrentlyModifiedException}.
     */
    @Override
    @Transactional
    public Auction save(Auction auction) {
        UUID id = auction.getId().value();
        AuctionEntity entity = jpaRepository.findById(id).orElseGet(() -> new AuctionEntity(id));
        if (entity.getVersion() != null && entity.getVersion() != auction.getVersion()) {
            throw new AuctionConcurrentlyModifiedException(auction.getId());
        }
        AuctionMapper.copyToEntity(auction, entity);
        try {
            return AuctionMapper.toDomain(jpaRepository.saveAndFlush(entity));
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new AuctionConcurrentlyModifiedException(auction.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Auction> findById(AuctionId id) {
        return jpaRepository.findById(id.value()).map(AuctionMapper::toDomain);
    }
}
