-- Soft delete only: rows are flagged with deleted = TRUE, never removed.

CREATE TABLE users (
    id         UUID PRIMARY KEY,
    full_name  VARCHAR(200)                NOT NULL,
    email      VARCHAR(320)                NOT NULL,
    phone      VARCHAR(16)                 NOT NULL,
    role       VARCHAR(20)                 NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    deleted    BOOLEAN                     NOT NULL DEFAULT FALSE
);

CREATE TABLE auctions (
    id                  UUID PRIMARY KEY,
    seller_id           UUID                        NOT NULL REFERENCES users (id),
    car_name            VARCHAR(200)                NOT NULL,
    car_condition       VARCHAR(20)                 NOT NULL,
    year_made           INTEGER                     NOT NULL,
    year_registered     INTEGER                     NOT NULL,
    description         VARCHAR(4000),
    start_price         NUMERIC(19, 2)              NOT NULL,
    current_highest_bid NUMERIC(19, 2),
    highest_bidder_id   UUID REFERENCES users (id),
    start_date          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    end_date            TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    status              VARCHAR(20)                 NOT NULL,
    won_by_user_id      UUID REFERENCES users (id),
    version             BIGINT                      NOT NULL,
    deleted             BOOLEAN                     NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_auctions_dates CHECK (end_date > start_date),
    CONSTRAINT ck_auctions_years CHECK (year_registered >= year_made),
    CONSTRAINT ck_auctions_start_price CHECK (start_price > 0)
);

CREATE INDEX ix_auctions_status_end_date ON auctions (status, end_date);

CREATE TABLE bids (
    id          UUID PRIMARY KEY,
    auction_id  UUID                        NOT NULL REFERENCES auctions (id),
    user_id     UUID                        NOT NULL REFERENCES users (id),
    given_price NUMERIC(19, 2)              NOT NULL,
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    deleted     BOOLEAN                     NOT NULL DEFAULT FALSE
);

CREATE INDEX ix_bids_auction_id ON bids (auction_id);
