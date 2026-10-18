package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.AuctionReadRepository;
import com.auction.domain.auction.AuctionStatus;
import com.auction.domain.auction.AuctionSummary;
import com.auction.domain.shared.Page;
import com.auction.domain.shared.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AuctionReadPersistenceAdapter implements AuctionReadRepository {

    /** endDate first, id as tie-breaker so paging is stable. */
    private static final Sort ENDING_SOONEST_FIRST = Sort.by("endDate", "id");

    private final AuctionJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<AuctionSummary> findById(AuctionId id) {
        return jpaRepository.findById(id.value()).map(AuctionMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuctionSummary> findRunning(Instant now, PageQuery pageQuery) {
        org.springframework.data.domain.Page<AuctionEntity> result =
                jpaRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThan(AuctionStatus.RUNNING,
                        now, now, PageRequest.of(pageQuery.page(), pageQuery.size(), ENDING_SOONEST_FIRST));
        return new Page<>(result.map(AuctionMapper::toSummary).getContent(), pageQuery.page(), pageQuery.size(),
                result.getTotalElements());
    }
}
