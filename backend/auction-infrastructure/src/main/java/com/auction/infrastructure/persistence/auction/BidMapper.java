package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.Bid;
import com.auction.domain.auction.BidId;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;

final class BidMapper {

    private BidMapper() {
    }

    static BidEntity toEntity(Bid bid) {
        BidEntity entity = new BidEntity(bid.id().value());
        entity.setAuctionId(bid.auctionId().value());
        entity.setUserId(bid.userId().value());
        entity.setGivenPrice(bid.givenPrice().amount());
        entity.setCreatedAt(bid.createdAt());
        return entity;
    }

    static Bid toDomain(BidEntity entity) {
        return new Bid(new BidId(entity.getId()), new AuctionId(entity.getAuctionId()), new UserId(entity.getUserId()),
                new Money(entity.getGivenPrice()), entity.getCreatedAt());
    }
}
