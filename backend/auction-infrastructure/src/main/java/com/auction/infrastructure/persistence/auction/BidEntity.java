package com.auction.infrastructure.persistence.auction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "bids")
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE bids SET deleted = true WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidEntity {

    @Id
    private UUID id;

    @Column(name = "auction_id", nullable = false)
    private UUID auctionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "given_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal givenPrice;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean deleted;

    public BidEntity(UUID id) {
        this.id = id;
    }
}
