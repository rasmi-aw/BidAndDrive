package com.auction.infrastructure;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boots only this module (JPA, Flyway, adapters) for integration tests.
 */
@SpringBootApplication
class PersistenceTestApplication {
}
