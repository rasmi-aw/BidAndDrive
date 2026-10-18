package com.auction.infrastructure.events;

import com.auction.domain.auction.AuctionClosed;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.BidPlaced;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends domain events to Kafka once the transaction that raised them has committed. Events raised outside a
 * transaction, or in one that rolls back, are never sent.
 * <p>
 * Delivery is at-most-once: if the broker is down right after the commit, the event is lost (logged).
 * A transactional outbox would make it at-least-once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = KafkaEventsConfig.ENABLED, havingValue = "true", matchIfMissing = true)
class KafkaDomainEventRelay {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(BidPlaced event) {
        send(AuctionTopics.BIDS, event.auctionId(), BidPlacedMessage.from(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(AuctionClosed event) {
        send(AuctionTopics.CLOSED, event.auctionId(), AuctionClosedMessage.from(event));
    }

    private void send(String topic, AuctionId auctionId, Object message) {
        String key = auctionId.value().toString();
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(message))
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Could not publish {} for auction {}", topic, key, error);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Could not serialize {} for auction {}", topic, key, e);
        }
    }
}
