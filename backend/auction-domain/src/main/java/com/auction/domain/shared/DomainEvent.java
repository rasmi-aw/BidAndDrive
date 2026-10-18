package com.auction.domain.shared;

import java.time.Instant;

/**
 * Something that happened in the domain, raised by an aggregate and published after its transaction commits.
 */
public interface DomainEvent {

    Instant occurredAt();
}
