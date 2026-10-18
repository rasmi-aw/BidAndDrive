package com.auction.infrastructure;

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
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class PersistenceFixtures {

    /** Truncated to micros: PostgreSQL timestamps keep microsecond precision. */
    public static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private PersistenceFixtures() {
    }

    public static User newSeller() {
        return newUser(Role.SELLER);
    }

    public static User newBuyer() {
        return newUser(Role.BUYER);
    }

    public static Auction newAuction(User seller, Instant startDate, Instant endDate) {
        return Auction.create(seller, new CarDetails("Peugeot 308", CarCondition.GOOD, Year.of(2018), Year.of(2019),
                "One owner"), Money.of("1000"), startDate, endDate);
    }

    /** Running auction, opened an hour ago, ending in a day. */
    public static Auction newRunningAuction(User seller) {
        return newAuction(seller, NOW.minus(Duration.ofHours(1)), NOW.plus(Duration.ofDays(1)));
    }

    private static User newUser(Role role) {
        int n = SEQUENCE.incrementAndGet();
        return User.register("User " + n, new Email("user" + n + "@example.com"),
                new Phone(String.format("+336%08d", n)), role, CLOCK);
    }
}
