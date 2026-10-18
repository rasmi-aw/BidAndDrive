package com.auction.domain;

import com.auction.domain.auction.Auction;
import com.auction.domain.auction.CarCondition;
import com.auction.domain.auction.CarDetails;
import com.auction.domain.shared.Money;
import com.auction.domain.user.Email;
import com.auction.domain.user.Phone;
import com.auction.domain.user.Role;
import com.auction.domain.user.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;

/**
 * Shared test data. All times are relative to {@link #NOW}.
 */
public final class DomainFixtures {

    public static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    public static final Instant START = NOW.minus(Duration.ofDays(1));
    public static final Instant END = NOW.plus(Duration.ofDays(7));
    public static final Money START_PRICE = Money.of("1000.00");

    private DomainFixtures() {
    }

    public static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    public static User seller() {
        return User.register("Sam Seller", new Email("seller@example.com"), new Phone("+33600000001"), Role.SELLER, clockAt(NOW));
    }

    public static User buyer() {
        return User.register("Bob Buyer", new Email("buyer@example.com"), new Phone("+33600000002"), Role.BUYER, clockAt(NOW));
    }

    public static CarDetails car() {
        return new CarDetails("Peugeot 308", CarCondition.GOOD, Year.of(2018), Year.of(2019), "One owner, full service history");
    }

    /** A RUNNING auction opened yesterday, ending in 7 days. */
    public static Auction runningAuction(User seller) {
        return Auction.create(seller, car(), START_PRICE, START, END);
    }
}
