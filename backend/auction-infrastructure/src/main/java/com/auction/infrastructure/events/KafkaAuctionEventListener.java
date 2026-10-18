package com.auction.infrastructure.events;

import com.auction.domain.auction.AuctionEventSubscriber;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Consumes auction events and hands them to the {@link AuctionEventSubscriber}s of this instance.
 * <p>
 * Every instance uses its own consumer group, so each one sees every event (needed to reach the browsers connected
 * to it), and starts from the latest offset: live updates only, no replay.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = KafkaEventsConfig.ENABLED, havingValue = "true", matchIfMissing = true)
class KafkaAuctionEventListener {

    private static final String GROUP = "auction-live-#{T(java.util.UUID).randomUUID()}";

    private final ObjectProvider<AuctionEventSubscriber> subscribers;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = AuctionTopics.BIDS, groupId = GROUP, properties = "auto.offset.reset=latest")
    void onBid(String payload) {
        read(payload, BidPlacedMessage.class)
                .ifPresent(message -> subscribers.orderedStream().forEach(s -> s.onBidPlaced(message.toEvent())));
    }

    @KafkaListener(topics = AuctionTopics.CLOSED, groupId = GROUP, properties = "auto.offset.reset=latest")
    void onClosed(String payload) {
        read(payload, AuctionClosedMessage.class)
                .ifPresent(message -> subscribers.orderedStream().forEach(s -> s.onAuctionClosed(message.toEvent())));
    }

    /** A malformed record is logged and skipped instead of being retried forever. */
    private <T> Optional<T> read(String payload, Class<T> type) {
        try {
            return Optional.of(objectMapper.readValue(payload, type));
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Skipping malformed {} record: {}", type.getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }
}
