package com.auction.domain.shared;

/**
 * Outbound port for domain events. Implementations must deliver an event only once the surrounding
 * transaction has committed, never for a rolled-back change.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
