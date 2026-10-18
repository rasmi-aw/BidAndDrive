package com.auction.infrastructure.persistence.auction;

import com.auction.domain.auction.AuctionStatus;
import com.auction.domain.auction.CarCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auctions")
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE auctions SET deleted = true WHERE id = ? AND version = ?")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionEntity {

    @Id
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "car_name", nullable = false, length = 200)
    private String carName;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_condition", nullable = false, length = 20)
    private CarCondition carCondition;

    @Column(name = "year_made", nullable = false)
    private int yearMade;

    @Column(name = "year_registered", nullable = false)
    private int yearRegistered;

    @Column(length = 4000)
    private String description;

    @Column(name = "start_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal startPrice;

    @Column(name = "current_highest_bid", precision = 19, scale = 2)
    private BigDecimal currentHighestBid;

    @Column(name = "highest_bidder_id")
    private UUID highestBidderId;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @Column(name = "won_by_user_id")
    private UUID wonByUserId;

    /** Optimistic lock: concurrent bids on the same auction cannot both commit. Managed by Hibernate only. */
    @Version
    @Setter(AccessLevel.PACKAGE)
    private Long version;

    @Column(nullable = false)
    private boolean deleted;

    public AuctionEntity(UUID id) {
        this.id = id;
    }
}
