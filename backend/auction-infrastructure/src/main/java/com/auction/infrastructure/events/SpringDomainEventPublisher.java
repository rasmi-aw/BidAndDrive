package com.auction.infrastructure.events;

import com.auction.domain.shared.DomainEvent;
import com.auction.domain.shared.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Hands domain events to Spring; {@code @TransactionalEventListener}s deliver them once the transaction commits.
 */
@Component
@RequiredArgsConstructor
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
