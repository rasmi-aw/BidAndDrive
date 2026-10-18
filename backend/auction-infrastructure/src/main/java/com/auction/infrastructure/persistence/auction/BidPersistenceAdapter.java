package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.Bid;
import com.auction.domain.auction.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class BidPersistenceAdapter implements BidRepository {

    private final BidJpaRepository jpaRepository;

    @Override
    @Transactional
    public Bid save(Bid bid) {
        return BidMapper.toDomain(jpaRepository.save(BidMapper.toEntity(bid)));
    }
}
