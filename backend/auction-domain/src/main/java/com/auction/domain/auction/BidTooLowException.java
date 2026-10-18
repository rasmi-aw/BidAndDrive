package com.auction.domain.auction;

import com.auction.domain.shared.DomainException;
import com.auction.domain.shared.Money;

public class BidTooLowException extends DomainException {

    public BidTooLowException(Money amount, Money minimumExclusive) {
        super("bid " + amount.amount() + " must be greater than " + minimumExclusive.amount());
    }
}
