package com.auction.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Base class for adapter tests against real PostgreSQL and Kafka (one container of each, shared by all test classes).
 */
@SpringBootTest(classes = PersistenceTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class InfrastructureIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    // same version as the kafka-clients managed by Spring Boot (Testcontainers 1.21 cannot start 4.x brokers)
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.2");

    static {
        POSTGRES.start();
        KAFKA.start();
    }

    protected static String kafkaBootstrapServers() {
        return KAFKA.getBootstrapServers();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // test-only reset between tests; production code never deletes rows
        jdbcTemplate.execute("TRUNCATE TABLE bids, auctions, users CASCADE");
    }
}
