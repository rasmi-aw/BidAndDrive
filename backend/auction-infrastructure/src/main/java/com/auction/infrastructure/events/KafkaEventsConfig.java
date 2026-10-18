package com.auction.infrastructure.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka event relay and consumer. {@code auction.kafka.enabled=false} switches all of it off
 * (events then stay in-process), e.g. for tests without a broker.
 */
@Configuration
@ConditionalOnProperty(name = KafkaEventsConfig.ENABLED, havingValue = "true", matchIfMissing = true)
class KafkaEventsConfig {

    static final String ENABLED = "auction.kafka.enabled";

    @Bean
    NewTopic auctionBidsTopic() {
        return TopicBuilder.name(AuctionTopics.BIDS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic auctionClosedTopic() {
        return TopicBuilder.name(AuctionTopics.CLOSED).partitions(3).replicas(1).build();
    }
}
