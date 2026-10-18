package com.auction.application.query;

import com.auction.domain.shared.Guard;
import com.auction.domain.shared.PageQuery;

public record ListRunningAuctionsQuery(PageQuery pageQuery) {

    public ListRunningAuctionsQuery {
        Guard.notNull(pageQuery, "pageQuery");
    }

    public static ListRunningAuctionsQuery of(int page, int size) {
        return new ListRunningAuctionsQuery(new PageQuery(page, size));
    }
}
