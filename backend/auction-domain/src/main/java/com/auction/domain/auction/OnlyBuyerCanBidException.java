package com.auction.domain.auction;

import com.auction.domain.shared.DomainException;
import com.auction.domain.user.UserId;

public class OnlyBuyerCanBidException extends DomainException {

    public OnlyBuyerCanBidException(UserId userId) {
        super("user " + userId.value() + " is not a BUYER and cannot bid");
    }
}
