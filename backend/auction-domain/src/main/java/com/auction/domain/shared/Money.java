package com.auction.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Non-negative monetary amount with a fixed scale of 2 decimals (single currency).
 */
public record Money(BigDecimal amount) {

    private static final int SCALE = 2;

    public Money {
        Guard.notNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new DomainValidationException("amount must not be negative: " + amount);
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new DomainValidationException("amount must have at most " + SCALE + " decimals: " + amount);
        }
        amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        return amount.compareTo(other.amount) > 0;
    }

    public Money max(Money other) {
        return other.isGreaterThan(this) ? other : this;
    }
}
