package com.auction.infrastructure.events;

import com.auction.domain.auction.AuctionClosed;
import com.auction.domain.auction.AuctionEventSubscriber;
import com.auction.domain.auction.AuctionId;
import com.auction.domain.auction.BidId;
import com.auction.domain.auction.BidPlaced;
import com.auction.domain.shared.DomainEventPublisher;
import com.auction.domain.shared.Money;
import com.auction.domain.user.UserId;
import com.auction.infrastructure.InfrastructureIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Real Kafka: events leave only after commit, land on the right topic, and come back to the subscribers.
 */
@Import(KafkaDomainEventsTest.RecordingSubscriberConfig.class)
class KafkaDomainEventsTest extends InfrastructureIntegrationTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private DomainEventPublisher publisher;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private RecordingSubscriber subscriber;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private KafkaListenerEndpointRegistry listeners;
    @Autowired
    private ObjectMapper objectMapper;

    private KafkaConsumer<String, String> rawConsumer;

    @BeforeEach
    void listenersReadyAndRawConsumer() {
        // the app consumes from the latest offset: wait until each listener owns all 3 partitions of its topic
        for (MessageListenerContainer container : listeners.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 3);
        }
        subscriber.events.clear();
        rawConsumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer());
        rawConsumer.subscribe(List.of(AuctionTopics.BIDS, AuctionTopics.CLOSED));
    }

    @AfterEach
    void closeRawConsumer() {
        if (rawConsumer != null) {
            rawConsumer.close();
        }
    }

    @Test
    void bidPlacedGoesToTheBidsTopicAfterCommitKeyedByAuction() throws Exception {
        BidPlaced event = bidPlaced();

        transactionTemplate.executeWithoutResult(status -> publisher.publish(event));

        ConsumerRecord<String, String> record = awaitRecord(event.auctionId()).orElseThrow();
        assertThat(record.topic()).isEqualTo(AuctionTopics.BIDS);
        JsonNode json = objectMapper.readTree(record.value());
        assertThat(json.get("auctionId").asText()).isEqualTo(event.auctionId().value().toString());
        assertThat(json.get("bidId").asText()).isEqualTo(event.bidId().value().toString());
        assertThat(json.get("bidderId").asText()).isEqualTo(event.bidderId().value().toString());
        assertThat(json.get("amount").decimalValue()).isEqualByComparingTo("1250.50");
        assertThat(Instant.parse(json.get("occurredAt").asText())).isEqualTo(NOW);
    }

    @Test
    void auctionClosedGoesToTheClosedTopic() {
        AuctionClosed event = new AuctionClosed(AuctionId.newId(), UserId.newId(), Money.of("1500"), NOW);

        transactionTemplate.executeWithoutResult(status -> publisher.publish(event));

        assertThat(awaitRecord(event.auctionId())).get()
                .extracting(ConsumerRecord::topic).isEqualTo(AuctionTopics.CLOSED);
    }

    @Test
    void nothingIsSentWhenTheTransactionRollsBack() {
        BidPlaced event = bidPlaced();

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publish(event);
            status.setRollbackOnly();
        });

        assertThat(awaitRecord(event.auctionId(), Duration.ofSeconds(3))).isEmpty();
    }

    @Test
    void nothingIsSentOutsideATransaction() {
        BidPlaced event = bidPlaced();

        publisher.publish(event);

        assertThat(awaitRecord(event.auctionId(), Duration.ofSeconds(3))).isEmpty();
    }

    @Test
    void committedEventsComeBackFromKafkaToTheSubscribers() {
        BidPlaced bid = bidPlaced();
        AuctionClosed closed = new AuctionClosed(bid.auctionId(), null, null, NOW);

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publish(bid);
            publisher.publish(closed);
        });

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(subscriber.events).contains(bid, closed));
    }

    @Test
    void aMalformedMessageIsSkippedAndTheNextOneIsDelivered() {
        BidPlaced next = bidPlaced();

        kafkaTemplate.send(new ProducerRecord<>(AuctionTopics.BIDS, next.auctionId().value().toString(), "{not json"));
        transactionTemplate.executeWithoutResult(status -> publisher.publish(next));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(subscriber.events).contains(next));
    }

    private static BidPlaced bidPlaced() {
        return new BidPlaced(AuctionId.newId(), BidId.newId(), UserId.newId(), Money.of("1250.50"), NOW);
    }

    private Optional<ConsumerRecord<String, String>> awaitRecord(AuctionId key) {
        return awaitRecord(key, Duration.ofSeconds(15));
    }

    private Optional<ConsumerRecord<String, String>> awaitRecord(AuctionId key, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            for (ConsumerRecord<String, String> record : rawConsumer.poll(Duration.ofMillis(250))) {
                if (record.key().equals(key.value().toString())) {
                    return Optional.of(record);
                }
            }
        }
        return Optional.empty();
    }

    static class RecordingSubscriber implements AuctionEventSubscriber {

        final List<Object> events = new CopyOnWriteArrayList<>();

        @Override
        public void onBidPlaced(BidPlaced event) {
            events.add(event);
        }

        @Override
        public void onAuctionClosed(AuctionClosed event) {
            events.add(event);
        }
    }

    @TestConfiguration
    static class RecordingSubscriberConfig {

        @Bean
        RecordingSubscriber recordingSubscriber() {
            return new RecordingSubscriber();
        }
    }

}
