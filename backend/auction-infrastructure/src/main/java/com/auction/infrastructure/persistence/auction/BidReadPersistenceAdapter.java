package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.Bid;
import com.auction.domain.auction.BidReadRepository;
import com.auction.domain.shared.Page;
import com.auction.domain.shared.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class BidReadPersistenceAdapter implements BidReadRepository {

    /** Newest first, id as tie-breaker so paging is stable. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final BidJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Bid> findByAuctionId(AuctionId auctionId, PageQuery pageQuery) {
        org.springframework.data.domain.Page<BidEntity> result = jpaRepository.findByAuctionId(auctionId.value(),
                PageRequest.of(pageQuery.page(), pageQuery.size(), NEWEST_FIRST));
        return new Page<>(result.map(BidMapper::toDomain).getContent(), pageQuery.page(), pageQuery.size(),
                result.getTotalElements());
    }
}
