package com.auction.domain.auction;

import com.auction.domain.shared.DomainException;
import com.auction.domain.user.UserId;

public class OnlySellerCanCreateAuctionException extends DomainException {

    public OnlySellerCanCreateAuctionException(UserId userId) {
        super("user " + userId.value() + " is not a SELLER and cannot create an auction");
    }
}
