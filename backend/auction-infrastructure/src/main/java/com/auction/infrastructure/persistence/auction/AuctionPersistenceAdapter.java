package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AuctionPersistenceAdapter implements AuctionRepository {

    private final AuctionJpaRepository jpaRepository;

    @Override
    @Transactional
    public Auction save(Auction auction) {
        UUID id = auction.getId().value();
        AuctionEntity entity = jpaRepository.findById(id).orElseGet(() -> new AuctionEntity(id));
        AuctionMapper.copyToEntity(auction, entity);
        return AuctionMapper.toDomain(jpaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Auction> findById(AuctionId id) {
        return jpaRepository.findById(id.value()).map(AuctionMapper::toDomain);
    }
}
